# Module Structure

## Top-Level Modules

### `card-common`

- 금액 같은 공통 값 객체
- 공통 예외와 공통 타입

### `card-core`

- 도메인 모델
- 유스케이스 인터페이스
- outbound port 인터페이스
- 추천 규칙과 실적 계산 규칙

### `card-api`

- Spring Boot 애플리케이션 진입점
- REST controller
- request/response DTO
- 거래 정규화와 데모 시나리오 로딩
- 정적 데모 UI와 조립 루트

### `card-infra`

- 현재 기준선의 인메모리 persistence adapter
- 이후 단계의 DB/외부 연동 adapter 확장 지점

## Current Package Layout

```text
card-common
└── com.jean202.cardmizer.common
    ├── Money
    └── PackageMarker

card-core
└── com.jean202.cardmizer.core
    ├── application
    │   ├── GetPerformanceOverviewService
    │   ├── RecommendCardService
    │   └── RecordSpendingService
    ├── domain
    │   ├── Card
    │   ├── CardId
    │   ├── CardPerformancePolicy
    │   ├── BenefitRule
    │   ├── PerformanceTier
    │   ├── PriorityStrategy
    │   ├── RecommendationContext
    │   ├── RecommendationResult
    │   ├── SpendingPeriod
    │   └── SpendingRecord
    └── port
        ├── in
        │   ├── RegisterCardUseCase
        │   ├── RecordSpendingUseCase
        │   ├── GetPerformanceOverviewUseCase
        │   ├── RecommendCardUseCase
        │   └── UpdatePriorityUseCase
        └── out
            ├── LoadCardCatalogPort
            ├── LoadCardPerformancePoliciesPort
            ├── LoadSpendingRecordsPort
            ├── SaveCardPort
            └── SaveSpendingRecordPort

card-api
├── src/main/java/com/jean202/cardmizer/api
│   ├── CardMizerApplication
│   ├── config
│   ├── demo
│   ├── normalization
│   ├── performance
│   ├── recommendation
│   └── spending
└── src/main/resources
    ├── demo
    ├── normalization
    └── static

card-infra
└── src/main/java/com/jean202/cardmizer/infra
    └── persistence
        ├── InMemoryCardCatalogAdapter
        ├── InMemoryCardPerformancePolicyAdapter
        └── InMemorySpendingRecordAdapter
```

## Dependency Direction

```text
card-api   --> card-core, card-infra
card-infra --> card-core
all        --> card-common
```

`card-api`는 조립 루트이고, `card-core`는 어떤 모듈에도 의존받을 뿐 직접 외부 구현에 의존하지 않는다.

## Current Notes

- `card-api`는 현재 정규화 규칙과 데모 시나리오 YAML도 함께 로드한다.
- `card-infra`는 아직 JPA 어댑터가 아니라 인메모리 구현만 포함한다.
- `cardcompany`, `notification` 같은 후속 adapter 슬라이스는 아직 생성하지 않았다.
