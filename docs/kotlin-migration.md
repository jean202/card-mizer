# Java → Kotlin 전환 기록

Java 17로 작성된 전체 소스코드(메인 91개 파일, 테스트 30개 파일)를 Kotlin으로 전환했습니다.  
단순 문법 변환이 아니라, 두 언어의 런타임·stdlib 차이로 인한 **실제 버그 4개를 발굴하고 수정**하는 과정을 포함합니다.

---

## 주요 변환 패턴

### Java Record → `@JvmInline value class`

```kotlin
// Before (Java record)
public record Money(long amount) {
    public static final Money ZERO = new Money(0L);
}

// After (Kotlin)
@JvmInline
value class Money(val amount: Long) {
    companion object {
        val ZERO = Money(0L)  // @JvmField 제거 — value class에 불허
        fun won(amount: Long): Money { ... }
    }
}
```

런타임에 boxing 없이 `Long`처럼 동작해 heap 할당이 사라집니다.

### Builder 패턴 → Named Parameters

```kotlin
// Before (Java)
BenefitRule.percentage("KPASS_TRANSIT", "대중교통 10% 결제일할인", 10)
    .categories("PUBLIC_TRANSIT")
    .minimumPreviousMonthSpent(Money.won(400_000))
    .monthlyCap(Money.won(5_000))
    .build()

// After (Kotlin)
BenefitRule(
    ruleId = "KPASS_TRANSIT",
    benefitSummary = "대중교통 10% 결제일할인",
    benefitType = BenefitType.RATE_PERCENT,
    rateBasisPoints = 1000,
    merchantCategories = setOf("PUBLIC_TRANSIT"),
    minimumPreviousMonthSpent = Money.won(400_000),
    monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(5_000))),
)
```

Builder 클래스 자체가 삭제되고, 어떤 필드에 어떤 값인지 한눈에 파악됩니다.

### `Optional<T>` → Nullable `T?`

```kotlin
// Before (Java)
Optional<BenefitQuote> quote = policy.estimateBenefit(...);
if (quote.isPresent()) { formatBenefitSuffix(quote.orElseThrow()); }

// After (Kotlin)
val quote: BenefitQuote? = policy.estimateBenefit(...)
quote?.let { formatBenefitSuffix(it) }
```

`Optional` boxing 비용이 없어지고, 컴파일러가 null 처리를 강제합니다.

### SAM 인터페이스 → `fun interface`

```kotlin
// Kotlin
fun interface LoadCardCatalogPort {
    fun loadAll(): List<Card>
}

// 테스트에서 명시적 SAM 생성자 필요
LoadCardCatalogPort { listOf(card1, card2) }
```

반환 타입이 `LoadCardCatalogPort`일 때 람다를 그냥 `{ ... }`로 쓰면 컴파일 에러가 납니다.  
명시적 SAM 생성자(`LoadCardCatalogPort { ... }`)를 써야 합니다.

---

## 발굴·수정한 버그 4개

### 버그 1 — `String.format()` + 문자열 보간 혼용 → `UnknownFormatConversionException`

**파일:** `card-core/.../RecommendCardService.kt`  
**증상:** 특정 카드 혜택명에서 추천 API가 500 에러 반환

```kotlin
// Before (잠재적 버그)
return " 예상 혜택은 %,d원(${quote.summary()})입니다.$capSuffix"
    .format(quote.benefitAmount.amount)
```

`quote.summary()`가 `"1% M포인트 적립"`을 반환하면, `${quote.summary()}`가 먼저 보간되어  
포맷 문자열 자체가 `"...(%M포인트...)"` 가 됩니다.  
이후 `.format()`이 `%M`을 포맷 지시자로 해석하려다 `UnknownFormatConversionException`이 발생합니다.

```kotlin
// After (수정)
val formattedAmount = "%,d".format(quote.benefitAmount.amount)
return " 예상 혜택은 ${formattedAmount}원(${quote.summary()})입니다.$capSuffix"
```

**근본 원인:** Kotlin 문자열 보간이 `.format()` 호출 전에 먼저 실행되는 것을 간과.  
Java에서는 `String.format()` 안에 `%s` 자리에 summary를 넣어 분리해 두었지만,  
Kotlin으로 옮기면서 보간(`${}`)과 포맷(`%,d`)을 한 식에 섞은 것이 원인.

---

### 버그 2 — `split(limit = -1)` → `IllegalArgumentException`으로 CSV 전체 행 실패

**파일:** `card-api/.../CsvImportController.kt`  
**증상:** CSV 임포트 시 모든 행이 에러로 집계되고 저장되는 행이 없음

```kotlin
// Before (잠재적 버그)
val columns = line.split(",", limit = -1)
```

Java의 `String.split(",", -1)`은 trailing empty string을 포함해 모두 분리합니다.  
그러나 Kotlin stdlib의 `String.split(vararg delimiters: String, limit: Int)`는  
`limit >= 0` 을 `require`로 강제하기 때문에 `IllegalArgumentException`을 던집니다.  
이 예외는 행별 try-catch 안에서 조용히 잡혀 에러 카운트만 누적됐습니다.

```kotlin
// After (수정)
val columns = line.split(",")  // Kotlin 기본 limit=0 → trailing empty 포함, 모두 분리
```

**근본 원인:** Java와 Kotlin의 `split` 시그니처가 다릅니다.  
Java는 `-1`을 "제한 없이 분리"로 처리하지만 Kotlin은 음수를 허용하지 않습니다.  
예외가 임포트 루프의 catch 블록에 묻혀 즉시 드러나지 않아 발견이 어려웠습니다.

---

### 버그 3 — `List<@Valid Foo>` 타입 인자 위치의 `@Valid` → 중첩 Bean Validation 미동작

**파일:** `card-api/.../CardPerformancePolicyController.kt`  
**증상:** 중첩 DTO(`PerformanceTierRequest`)의 필드 검증이 실행되지 않아, 잘못된 값이 도메인 생성자까지 도달해 500 에러 발생

```kotlin
// Before (버그)
data class ReplaceCardPerformancePolicyRequest(
    @field:NotEmpty
    val tiers: List<@Valid PerformanceTierRequest>,  // 타입 인자 위치
)
```

Java에서는 `List<@Valid Foo>`의 type-use 어노테이션을 Hibernate Validator가 처리합니다.  
Kotlin에서는 이 위치의 어노테이션이 JVM 바이트코드에서 Hibernate Validator가 읽는 방식으로 전달되지 않아,  
`PerformanceTierRequest`에 달린 `@NotBlank`, `@PositiveOrZero` 등이 **전혀 실행되지 않았습니다.**

결과적으로 `targetAmount: -1`이 검증을 통과하고 `Money.won(-1)` 호출까지 도달해  
`IllegalArgumentException`이 터지면서 기대한 `fieldErrors` 응답 대신 다른 형식의 400이 반환됐습니다.

```kotlin
// After (수정)
data class ReplaceCardPerformancePolicyRequest(
    @field:NotEmpty
    @field:Valid  // 프로퍼티 필드 레벨로 이동
    val tiers: List<PerformanceTierRequest>,
)
```

**근본 원인:** Kotlin과 Java의 어노테이션 타깃 처리 방식 차이.  
Kotlin에서 `List<@Valid Foo>`의 `@Valid`는 type argument annotation으로 처리되지만  
이 정보가 Hibernate Validator에게 필요한 JVM 메타데이터로 전달되지 않습니다.  
`@field:Valid`를 컬렉션 프로퍼티에 직접 붙여야 cascade validation이 동작합니다.

---

### 버그 4 — `kotlin-reflect` / `jackson-module-kotlin` 누락 → Spring 컨텍스트 로드 실패 및 역직렬화 오류

**파일:** `build.gradle.kts`, `card-infra/build.gradle.kts`

**증상 A:** `ClassNotFoundException: kotlin.reflect.full.KClasses` → Spring Data JPA 기동 불가  
**증상 B:** `InvalidDefinitionException: no Creators` → JPA 어댑터의 JSON 컬럼 역직렬화 실패

```kotlin
// build.gradle.kts 루트 — 추가
add("implementation", "org.jetbrains.kotlin:kotlin-reflect")

// card-infra/build.gradle.kts — 추가
implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
```

**근본 원인 A:** Java 프로젝트에선 불필요했지만,  
Kotlin + Spring Data JPA 조합은 `kotlin-reflect`가 있어야 생성자를 분석할 수 있습니다.

**근본 원인 B:** `JpaCardPerformancePolicyAdapter`의 내부 document 클래스가  
Java record → Kotlin data class로 바뀌었습니다.  
Jackson은 Java record를 기본 지원하지만, Kotlin data class는 `jackson-module-kotlin` 없이는  
no-arg constructor가 없다는 이유로 역직렬화에 실패합니다.

---

## 의존성 변경 요약

| 항목 | 이유 |
|------|------|
| `kotlin-reflect` 추가 (루트) | Spring Data JPA가 Kotlin 생성자 분석에 필요 |
| `jackson-module-kotlin` 추가 (card-infra) | Java record → data class 전환으로 Jackson 역직렬화 지원 필요 |

## 인터뷰 핵심 포인트

이 전환 작업의 핵심은 **언어 간 미묘한 동작 차이가 숨어 있던 버그를 수면 위로 끌어올렸다**는 점입니다.

- 버그 1, 2는 자바 코드에서도 동일하게 존재했지만, Kotlin으로 옮기는 과정에서 테스트가 실제로 실행되면서 드러남
- 버그 3은 Java의 type-use 어노테이션 처리와 Kotlin JVM 바이트코드 표현의 차이
- 버그 4는 Java record의 Jackson 기본 지원과 Kotlin data class의 차이

단순히 문법을 옮기는 것이 아니라, **두 언어의 런타임·stdlib·프레임워크 통합 동작 방식을 이해해야** 올바른 Kotlin 코드를 작성할 수 있음을 실증한 사례입니다.
