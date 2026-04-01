# card-mizer

카드 실적, 혜택 정책, 우선순위를 계산해 결제 카드를 추천하는 Java/Spring 백엔드 데모입니다.

## Problem

여러 장의 카드를 함께 쓰면 이번 달 실적이 어디까지 찼는지, 다음 결제를 어느 카드에 태워야 유리한지 매번 직접 계산해야 합니다. `card-mizer`는 이 판단 비용을 줄이기 위해, 실적 추적과 추천 로직을 도메인 모델 중심으로 풀어내는 프로젝트입니다.

## What This Repository Proves

- Java/Spring 기반 도메인 로직 설계 및 구현 역량
- 헥사고날 아키텍처와 Gradle 멀티모듈 적용 능력
- 단순 CRUD보다 도메인 판단 로직이 중심인 백엔드 모델링
- 거래 정규화와 카드 혜택 규칙 모델링 능력
- 외부 API 연동 어댑터 설계 (인증, 재시도, 에러 핸들링)
- 문서와 테스트로 설계 근거를 남기는 습관

## Current State

- `card-core`에서 사용 내역 기록, 실적 조회, 결제 추천, 카드 등록, 우선순위 조정, 카드 정책 조회/교체, 카드사 거래 동기화 유스케이스를 구현했습니다.
- `card-api`에서 REST API와 정적 데모 UI를 함께 제공합니다. 카드사 API 시뮬레이터도 포함되어 있습니다.
- `card-infra`는 JPA/H2 기반 영속성 어댑터와 외부 카드사 API 호출 어댑터를 제공합니다.
- 스키마는 Flyway migration으로 관리하고, 기본 프로파일은 `h2`입니다.
- 신규 카드 등록 시 기본 실적 정책을 함께 생성해 추천/실적 조회 흐름이 깨지지 않도록 맞췄습니다.
- 가맹점/태그 정규화 규칙과 추천 시나리오는 YAML fixture로 관리합니다.
- `./gradlew test` 기준 단위·통합 테스트(25개 파일)가 통과합니다.

## Architecture Snapshot

```mermaid
flowchart LR
    CLIENT["Browser / REST Client"] --> API["card-api<br/>REST API + Demo UI + Simulator"]
    API --> CORE["card-core<br/>Domain + Use Cases + Ports"]
    API --> INFRA["card-infra<br/>JPA + External API Adapters"]
    INFRA --> CORE
    INFRA -->|HTTP| SIM["Card Company Simulator<br/>(card-api /simulator)"]
    COMMON["card-common<br/>Money + Shared Types"] --> CORE
    COMMON --> API
    COMMON --> INFRA
```

`card-api`가 시연·조립·시뮬레이터를 맡고, 핵심 추천 규칙은 `card-core`에 남겨 두며, 저장은 JPA 어댑터, 외부 거래 조회는 카드사 API 어댑터가 담당합니다.

## Demo Flow

브라우저에서 `http://localhost:8080`에 접속하면 다음 흐름을 바로 시연할 수 있습니다.

1. 추천 시나리오 선택
2. 추천 요청값 자동 채움
3. 추천 실행
4. 추천 카드와 대안 카드 확인
5. 같은 월의 실적 현황 조회
6. 카드사 거래 동기화 (시뮬레이터 → 어댑터 → DB)
7. 카드 정책 폼 기반 편집

기본 시나리오는 다음 4개가 준비돼 있습니다.

- `K-패스 실적 달성과 대중교통 할인`
- `노리2 영화 할인과 KB Pay 추가 할인 중첩`
- `My WE:SH 노는데 진심과 OTT 할인`
- `현대 ZERO 포인트형 온라인 간편결제`

## Quick Start

```bash
./gradlew test
./gradlew :card-api:bootRun
```

- 데모 UI: `http://localhost:8080`
- 추천 시나리오 API: `GET /api/demo-scenarios/recommendations`

PostgreSQL로 실행하려면 저장소의 로컬 PostgreSQL을 먼저 띄운 뒤 `postgres` 프로파일로 부팅하면 됩니다.

```bash
docker compose up -d
./gradlew :card-api:bootRun --args='--spring.profiles.active=postgres'
```

기본 연결 정보는 `jdbc:postgresql://localhost:55432/cardmizer`, `cardmizer/cardmizer` 입니다. 다른 DB를 쓰려면 `CARD_MIZER_DB_URL`, `CARD_MIZER_DB_USERNAME`, `CARD_MIZER_DB_PASSWORD`를 덮어쓰면 됩니다.

## API Surface

| Endpoint | Purpose |
|------|------|
| `GET /api/demo-scenarios/recommendations` | 데모 시나리오 목록과 기본 요청값 조회 |
| `POST /api/recommendations` | 결제 금액 기준 추천 카드 계산 |
| `GET /api/performance-overview?yearMonth=YYYY-MM` | 월별 카드 실적 현황 조회 |
| `POST /api/spending-records` | 수동 사용 내역 저장 |
| `POST /api/cards` | 카드 등록과 기본 실적 정책 생성 |
| `PATCH /api/cards/priorities` | 등록된 카드 전체의 우선순위 재정렬 |
| `GET /api/cards/{cardId}/performance-policy` | 카드별 실적/혜택 정책 조회 |
| `PUT /api/cards/{cardId}/performance-policy` | 카드별 실적/혜택 정책 전체 교체 |
| `PATCH /api/cards/{cardId}/performance-policy` | 카드별 실적/혜택 정책 일부 필드만 교체 |
| `GET /api/cards` | 등록된 전체 카드 목록 조회 |
| `POST /api/sync/transactions` | 카드사 API에서 거래 내역 동기화 |
| `GET /simulator/api/v1/cards/{cardId}/transactions` | 카드사 API 시뮬레이터 (X-Api-Key 필수) |

## Example Request

아래 예시는 시드 시나리오 중 `kpass-transit-threshold`를 그대로 호출하는 요청입니다.

```bash
curl -X POST http://localhost:8080/api/recommendations \
  -H 'Content-Type: application/json' \
  -d '{
    "spendingMonth": "2026-03",
    "amount": 20000,
    "merchantName": "서울교통공사",
    "merchantCategory": "대중교통",
    "paymentTags": []
  }'
```

```json
{
  "recommendedCardId": "SAMSUNG_KPASS",
  "recommendedCardName": "삼성카드 K-패스 삼성카드",
  "reason": "삼성카드 K-패스 삼성카드 KPASS_40 구간까지 20,000원 남아 있어 이번 결제로 바로 달성할 수 있습니다. 예상 혜택은 2,000원(대중교통 10% 결제일할인)입니다.",
  "alternatives": [
    {
      "cardId": "HYUNDAI_ZERO_POINT",
      "cardName": "현대카드 ZERO Edition2(포인트형)",
      "reason": "...",
      "score": 0
    }
  ]
}
```

위 응답 예시는 필드 구조 설명용으로 일부 대안 항목을 축약했습니다.

## Example Overview Response

```bash
curl "http://localhost:8080/api/performance-overview?yearMonth=2026-03"
```

```json
[
  {
    "cardId": "SAMSUNG_KPASS",
    "cardName": "삼성카드 K-패스 삼성카드",
    "priority": 1,
    "spentAmount": 380000,
    "targetAmount": 400000,
    "remainingAmount": 20000,
    "achieved": false,
    "targetTierCode": "KPASS_40"
  }
]
```

## Normalization Rules

입력은 그대로 쓰지 않고 API 계층에서 먼저 정규화합니다.

- `merchantCategory: "대중교통"` -> `PUBLIC_TRANSIT`
- `merchantName: "CGV 왕십리"` -> `MOVIE`
- `paymentTags: ["KB Pay"]` -> `KB_PAY`
- 카테고리와 태그를 합쳐 `ONLINE`, `OFFLINE`, `SUBSCRIPTION`, `SIMPLE_PAY_ONLINE` 같은 파생 태그도 계산합니다.

이 단계 덕분에 추천 엔진은 카드사/가맹점 표현 차이보다 도메인 규칙 자체에 집중할 수 있습니다.

## Architecture At A Glance

- `card-common`: 공통 값 객체와 공통 타입
- `card-core`: 도메인 모델, 유스케이스, 포트 (프레임워크 의존 없음)
- `card-api`: REST API, 정규화, 데모 시나리오, 카드사 시뮬레이터, 정적 UI, 조립 루트
- `card-infra`: JPA 영속성 어댑터 + 외부 카드사 API 어댑터
- `db/migration`: Flyway schema migration

핵심 추천 로직은 `card-core`에 남겨 두고, 프레임워크 의존성은 바깥으로 밀어냈습니다. 외부 카드사 API 연동은 `FetchCardTransactionsPort` 아웃바운드 포트를 통해 이루어지며, 시뮬레이터를 실제 API로 교체할 때 어댑터만 변경하면 됩니다.

## Testing

- `RecommendCardServiceTest`: 실적 달성 우선과 혜택 우선이 섞이는 추천 규칙 검증
- `GetPerformanceOverviewServiceTest`: 월별 실적 집계와 다음 목표 구간 계산 검증
- `SyncCardTransactionsServiceTest`: 외부 API 거래 동기화 흐름 검증
- `CardCompanySimulatorControllerTest`: 시뮬레이터 인증, 월별 데이터 변동, 에러 시뮬레이션 검증
- `SyncFlowIntegrationTest`: 시뮬레이터 → 어댑터 → 서비스 end-to-end 검증
- `TransactionNormalizerTest`: 가맹점/태그 정규화 규칙 검증
- `RecommendationDemoScenarioFixtureTest`: YAML 시나리오가 기대한 추천 결과를 실제로 만드는지 검증

## Deferred Work

- 운영용 PostgreSQL 설정과 Testcontainers 기반 통합 테스트
- 다중 사용자 지원 (인증/인가)
- 실적 달성 임박 알림 시스템

## Stack

- Java 17
- Spring Boot 3.4
- Gradle Multi-module
- Spring Web + Spring Data JPA
- Flyway (schema migration)
- H2 (기본) / PostgreSQL (프로파일)
- java.net.http.HttpClient (외부 API 어댑터)
- Static HTML/CSS/JS demo
- JUnit 5

## Docs

- `PROJECT_PLAN.md`: 현재 결정 사항 요약
- `docs/product-overview.md`: 이름 후보, 포지셔닝, MVP 범위
- `docs/domain-model.md`: 핵심 도메인 모델
- `docs/architecture.md`: 헥사고날 아키텍처 설명
- `docs/module-structure.md`: 모듈/패키지 구조
- `docs/adr/README.md`: ADR 목록과 상태
- `docs/delivery-plan.md`: 다음 마일스톤과 작업 큐
- `docs/work-log-2026-03-31.md`: 2026-03-31 구현/검증 복기 노트

## Status

Flyway + JPA/H2 기반으로 동작하는 추천/관리/카드사 연동 데모입니다. 헥사고날 아키텍처의 어댑터 교체를 카드사 API 시뮬레이터와 외부 API 어댑터로 실제 증명합니다.
