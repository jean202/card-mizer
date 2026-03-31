# 작업 기록: 2026-03-31 포트폴리오 마무리 세션

## 세션 목표

기획 문서와 실제 코드 사이의 갭을 파악하고, 포트폴리오 완성도를 높이는 작업을 수행했다.

---

## 1. 프로젝트 현황 분석

### 무엇을 했는가

기획 문서(`PROJECT_PLAN.md`, `docs/delivery-plan.md`)에 적힌 "할 일 목록"과 실제 코드를 대조해서 이미 완료된 항목과 남은 항목을 구분했다.

### 분석 결과

| 기획 문서에 적힌 항목 | 실제 상태 |
|---|---|
| API 입력 검증 & 에러 응답 | 이미 `@Valid` + `ApiExceptionHandler`로 구현됨 |
| 카드 등록 / 우선순위 조정 | 이미 엔드포인트 + 테스트 완료 |
| 카드 정책 부분 수정 (PATCH) | 이미 구현 + 통합 테스트 완료 |
| PostgreSQL 프로파일 | `compose.yaml` + `application-postgres.properties` 준비됨 |
| API 문서 | **없음** — springdoc/swagger 의존성 없었음 |
| 설계 근거 문서 (ADR) | ADR 2개만 존재, 핵심 도메인 판단 ADR 없었음 |

### 공부 포인트

> **기획 문서는 코드보다 빨리 낡는다.** 기능을 구현한 뒤 기획 문서를 갱신하지 않으면, 문서를 읽는 사람(면접관, 협업자)이 실제 완성도를 과소평가하게 된다. 정기적으로 "문서에 적힌 것 vs 실제 코드"를 대조하는 습관이 필요하다.

---

## 2. API 문서 도구 선정: springdoc-openapi + Scalar UI

### 비교한 선택지

| 도구 | 장점 | 단점 |
|---|---|---|
| **Spring REST Docs** | 테스트 기반 → 문서 정확성 보장, 프로덕션 코드 오염 없음 | 설정 복잡, 인터랙티브 UI 없음 |
| **springdoc-openapi (Swagger UI)** | 의존성 추가만으로 동작, 브라우저에서 API 테스트 가능 | 컨트롤러에 어노테이션 오염, UI가 구식 |
| **springdoc-openapi + Scalar UI** | Swagger UI보다 세련된 UI, springdoc 공식 지원 | Scalar가 상대적으로 신생 도구 |
| **Epages restdocs-api-spec** | REST Docs + OpenAPI spec 동시 생성 | 설정이 가장 복잡, 마이너 라이브러리 |

### 왜 springdoc + Scalar를 골랐는가

1. **포트폴리오 데모 목적**: 방문자가 브라우저에서 바로 API를 시험해볼 수 있어야 함 → 인터랙티브 UI 필수
2. **설정 비용**: 의존성 한 줄 + HTML 한 장이면 끝 → REST Docs 대비 진입 비용이 낮음
3. **트렌드**: Microsoft가 .NET 9에서 Swagger UI를 Scalar로 교체, springdoc 공식 모듈 존재
4. **차별화**: 대부분의 포트폴리오가 Swagger UI를 쓰므로, Scalar를 쓰면 "최신 도구를 알고 적용한다"는 인상

### 공부 포인트

> **도구 선정은 "최고의 도구"가 아니라 "맥락에 맞는 도구"를 고르는 것이다.** 상용 서비스라면 REST Docs(테스트 기반 정확성)가 더 나을 수 있고, 데모 프로젝트라면 Scalar(시각적 인상)가 더 나을 수 있다. 선정 기준을 먼저 정하고 비교하는 순서가 중요하다.

---

## 3. springdoc-openapi + Scalar UI 적용

### 변경한 파일들

```
card-api/build.gradle.kts                              ← 의존성 추가
card-api/src/main/resources/application.properties      ← Swagger UI 비활성화
card-api/src/main/java/.../config/OpenApiConfiguration.java  ← 신규: API 메타정보
card-api/src/main/resources/static/api-docs.html        ← 신규: Scalar UI 페이지
```

### 3-1. 의존성 추가

```kotlin
// card-api/build.gradle.kts
implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.6")
```

**왜 `webmvc-api`인가:**
- `springdoc-openapi-starter-webmvc-ui`: Swagger UI를 함께 번들링 (불필요)
- `springdoc-openapi-starter-webmvc-api`: OpenAPI spec 생성만 담당 (Scalar UI는 별도 HTML로 제공)

Scalar UI를 쓸 것이므로 Swagger UI 번들이 필요 없다. `-api` 아티팩트만 가져오면 `/v3/api-docs` 엔드포인트가 자동 생성된다.

### 3-2. OpenAPI 메타정보 설정

```java
// OpenApiConfiguration.java
@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Card Mizer API")
                        .version("0.1.0")
                        .description("카드 실적과 혜택 우선순위를 계산해 결제 카드를 추천하는 API"));
    }
}
```

**동작 원리:**
- springdoc은 Spring Boot 자동 구성으로 모든 `@RestController`를 스캔한다
- 컨트롤러의 `@GetMapping`, `@PostMapping` 등에서 경로, HTTP 메서드, 파라미터를 추출한다
- `@Valid`가 붙은 request DTO의 `@NotBlank`, `@Positive` 등 검증 어노테이션도 스키마에 반영한다
- 위 `OpenAPI` 빈은 생성된 spec의 제목, 버전, 설명을 덮어쓴다

### 3-3. Scalar UI 페이지

```html
<!-- api-docs.html -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Card Mizer API Reference</title>
</head>
<body>
<script id="api-reference" data-url="/v3/api-docs"></script>
<script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
</body>
</html>
```

**동작 원리:**
1. 브라우저가 `/api-docs.html`을 로드한다
2. Scalar 스크립트가 `data-url`에 지정된 `/v3/api-docs`에서 OpenAPI JSON을 가져온다
3. JSON을 파싱해서 인터랙티브 API 문서 UI를 렌더링한다

Spring Boot의 정적 리소스 서빙(`src/main/resources/static/`)을 그대로 활용하므로 별도 설정이 필요 없다.

### 3-4. Swagger UI 비활성화

```properties
# application.properties
springdoc.swagger-ui.enabled=false
```

`-api` 아티팩트를 쓰면 Swagger UI가 포함되지 않지만, 명시적으로 비활성화해두면 나중에 의존성이 바뀌어도 의도가 보존된다.

### 검증 방법

```bash
./gradlew :card-api:bootRun
# 브라우저에서 확인:
# - http://localhost:8080/v3/api-docs     → OpenAPI JSON
# - http://localhost:8080/api-docs.html   → Scalar UI
```

### 공부 포인트

> **springdoc의 자동 스캔 원리를 이해하면** `@Operation`, `@Schema` 같은 어노테이션 없이도 대부분의 API 문서가 자동 생성되는 이유를 알 수 있다. springdoc은 Spring MVC의 `RequestMappingHandlerMapping`에서 등록된 핸들러 메서드를 순회하며 OpenAPI spec을 구성한다. 어노테이션은 자동 생성된 결과를 "보완"하는 역할이다.

---

## 4. 기획 문서 갱신

### 변경한 파일들

```
PROJECT_PLAN.md
docs/delivery-plan.md
```

### 갱신 원칙

1. **이미 완료된 항목은 "할 일"에서 제거한다** — "카드 등록 구현"이 Immediate Priorities에 남아있으면 안 됨
2. **현재 빌드 상태를 정확히 반영한다** — API 검증, PATCH, PostgreSQL 준비 상태 등
3. **다음 우선순위를 현실적으로 재정렬한다** — 구현이 아닌 문서화/테스트 보강이 남은 작업

### 공부 포인트

> **Delivery Plan은 "무엇을 했는가"가 아니라 "다음에 무엇을 할 것인가"를 위한 문서다.** 완료된 항목을 지우지 않으면 문서가 점점 길어지고, 읽는 사람이 현재 상태를 파악하기 어려워진다. "Current Milestone Status"는 항상 현재 시점의 스냅샷이어야 한다.

---

## 5. ADR (Architecture Decision Record) 작성

### ADR이란

소프트웨어 설계에서 내린 **핵심 판단과 그 근거**를 기록하는 짧은 문서다. 형식은 보통:

```
# 제목
## Status — Accepted / Deprecated / Superseded
## Context — 왜 이 판단이 필요했는가
## Decision — 무엇을 결정했는가
## Consequences — 이 결정의 결과와 트레이드오프
```

### 작성한 ADR 4개

#### ADR-003: 카드사 연동 전 수동 입력으로 시작

**핵심 판단**: 데이터 입력을 카드사 API가 아닌 REST API 수동 입력으로 시작한다.

**이 ADR이 보여주는 것**: MVP 범위 판단 능력. "지금 당장 필요한 것"과 "나중에 붙일 수 있는 것"을 구분하는 사고방식.

**트레이드오프**:
- 얻는 것: 외부 의존성 없이 추천 엔진을 바로 검증
- 잃는 것: 사용자 편의성 (직접 입력해야 함)
- 완화 방법: 데모 시드 데이터로 보완

#### ADR-004: 3구간 가중치 기반 추천 스코어링

**핵심 판단**: 카드 점수를 3개 구간(달성 가능 / 진행 중 / 이미 완료)으로 나누고, 구간 간 점수 격차를 크게 둔다.

**이 ADR이 보여주는 것**: 도메인 문제를 알고리즘으로 풀어내는 능력. "실적 달성 카드가 항상 우선"이라는 비즈니스 규칙을 구간 분리로 구조적으로 보장.

**핵심 설계 포인트**:
```
Tier 1 최저(10,000) > Tier 2 최고(~6,600)
→ 어떤 보정을 적용해도 구간 역전이 불가능
→ 비즈니스 규칙이 수학적으로 보장됨
```

**트레이드오프**:
- 얻는 것: 추천 의도가 명확하고 예측 가능
- 잃는 것: 보정 계수가 하드코딩 (튜닝 시 코드 변경 필요)

#### ADR-005: 카드 정책을 JSON으로 저장

**핵심 판단**: `CardPerformancePolicy`를 정규화된 5~7개 테이블이 아닌, 카드당 한 행에 JSON 두 컬럼으로 저장한다.

**이 ADR이 보여주는 것**: 저장소 설계에서 트레이드오프를 의식적으로 선택하는 능력.

**비교**:
```
정규화 테이블:
  ✓ 규칙 단위 쿼리 가능 (WHERE category = 'MOVIE')
  ✗ 스키마 변경 시 마이그레이션 필요
  ✗ 정책 조회 시 5~7개 JOIN

JSON 저장:
  ✓ 정책 조회가 단일 행
  ✓ 필드 추가 시 마이그레이션 불필요
  ✗ 규칙 내부 기준 검색 비효율
```

현재 코드는 "카드 X의 정책 전체 조회"가 주 패턴이므로 JSON이 적합하다.

#### ADR-006: 배타 그룹과 공유 한도 포함 혜택 추정

**핵심 판단**: 단순 할인율 적용이 아니라, 카드사 약관의 실제 제약(배타 선택, 공유 한도, 구간별 상한, 횟수 제한)을 모델링한다.

**이 ADR이 보여주는 것**: 실제 도메인의 복잡성을 직시하고 모델로 옮기는 능력.

**혜택 평가 3단계**:
```
1. 이력 시뮬레이션 → 규칙별 누적 상태 추적
2. 배타 그룹 선별  → 그룹 내 최대 혜택 규칙만 남김
3. 한도 적용       → 규칙별 한도 → 공유 그룹 한도 순서로 차감
```

**트레이드오프**:
- 얻는 것: 혜택 과대 추정 방지, 추천 신뢰도 향상
- 잃는 것: 매 추천마다 O(연간 거래 수 × 규칙 수) 비용

### 공부 포인트

> **ADR은 "코드에 안 보이는 판단"을 기록하는 도구다.** 코드를 읽으면 "무엇을 했는지"는 알 수 있지만, "왜 다른 방법을 쓰지 않았는지"는 알 수 없다. Consequences 절에 트레이드오프를 적어두면 나중에 자신이나 팀원이 "이거 왜 이렇게 했지?"라고 물을 때 답이 된다.

> **좋은 ADR의 특징**: Context에서 선택지를 나열하고, Decision에서 하나를 고르고, Consequences에서 "얻는 것"과 "잃는 것"을 둘 다 적는다. "이게 최고의 결정이다"가 아니라 "이 맥락에서 이 트레이드오프를 선택했다"를 보여주는 것이 핵심이다.

---

## 오늘 변경된 파일 전체 목록

| 파일 | 변경 유형 | 목적 |
|---|---|---|
| `card-api/build.gradle.kts` | 수정 | springdoc-openapi 의존성 추가 |
| `card-api/src/main/resources/application.properties` | 수정 | Swagger UI 비활성화 |
| `card-api/src/.../config/OpenApiConfiguration.java` | 신규 | API 메타정보 설정 |
| `card-api/src/main/resources/static/api-docs.html` | 신규 | Scalar UI 페이지 |
| `PROJECT_PLAN.md` | 수정 | 완료 항목 반영, 우선순위 갱신 |
| `docs/delivery-plan.md` | 수정 | 마일스톤 상태, 작업 큐, 종료 기준 갱신 |
| `docs/adr/ADR-003-...manual-input...md` | 신규 | 수동 입력 우선 전략 ADR |
| `docs/adr/ADR-004-...scoring...md` | 신규 | 추천 스코어링 알고리즘 ADR |
| `docs/adr/ADR-005-...json...md` | 신규 | 정책 JSON 저장 ADR |
| `docs/adr/ADR-006-...benefit-estimation...md` | 신규 | 혜택 추정 모델 ADR |
| `docs/adr/README.md` | 수정 | ADR 인덱스 갱신 |

---

## 다음 단계

포트폴리오 마무리 이후, 상용화 Phase 1 범위를 구체화할 예정이다.

```
Phase 0 (현재) — 포트폴리오 데모 ✅
Phase 1         — 상용 MVP (인증 + PostgreSQL 운영 + 카드 정보 관리)
Phase 2         — 데이터 연동 (마이데이터/스크래핑)
Phase 3         — 사용자 확장 (모바일, 알림, 개인화)
```
