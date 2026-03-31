# Domain Model

## Core Questions

- 이번 달 각 카드 실적은 현재 어디까지 도달했는가
- 특정 금액을 결제할 때 어떤 카드가 가장 유리한가
- 실적 달성 우선과 즉시 혜택 우선이 충돌할 때 어떤 기준으로 선택할 것인가

## Primary Domain Types

| 타입 | 역할 | 비고 |
|------|------|------|
| `Card` | 카드 기본 정보와 현재 우선순위 | 카드 자체의 정적 속성 |
| `CardPerformancePolicy` | 카드별 실적 구간과 달성 기준 | 카드별 정책의 집합 |
| `PerformanceTier` | 하나의 실적 구간 | 예: 30만원 달성 시 혜택 |
| `BenefitRule` | 카드사별 실제 혜택 규칙 | 정률/정액, 전월 실적, 거래 태그, 월/연 한도, 공유 한도, 횟수 제한 포함 |
| `BenefitMonthlyCapTier` | 전월 실적 구간별 혜택 한도 | 예: 40만원 이상 4천원, 80만원 이상 8천원 |
| `AppliedBenefit` / `BenefitQuote` | 한 거래에 적용된 혜택 묶음 | KB Pay 중복 할인처럼 복수 규칙 적용 가능 |
| `SpendingRecord` | 월간 사용 내역 | 수동 입력이 MVP 기준, `paymentTags` 포함 |
| `SpendingPeriod` | 기준 월 | 월간 리셋 경계 표현 |
| `PriorityStrategy` | 카드 우선순위 순서 | 월별로 바뀔 수 있음 |
| `RecommendationContext` | 추천 계산 입력 값 | 금액, 가맹점, 기준 월, 거래 태그 |
| `RecommendationResult` | 추천 결과와 사유 | 추천 카드와 대안 포함 |

## Current Implementation Notes

- 현재 baseline은 4개의 시드 카드와 인메모리 카드 정책/사용 내역 저장소를 기준으로 동작한다.
- `BenefitRule`은 단순 카테고리 비교를 넘어 거래 태그, 전월 실적, 월/연 한도, 공유 한도, 횟수 제한까지 반영한다.
- 추천 계산은 현재월, 전월, 연초부터 현재월까지의 기록을 함께 참조한다.
- 가맹점명과 태그 입력은 API 계층에서 정규화한 뒤 도메인으로 전달한다.

## Aggregate View

- `Card` aggregate
  현재는 카드 메타데이터와 우선순위를 함께 관리한다.
- `CardPerformancePolicy` aggregate
  카드별 실적 구간과 혜택 설명을 소유한다.
- `SpendingRecord` aggregate
  월간 누적 실적 계산의 원천 데이터다.

## Recommendation Logic Outline

1. 대상 기간의 카드 목록과 우선순위를 로드한다.
2. 현재 월, 전월, 연초부터 현재 월까지의 사용 내역을 함께 로드한다.
3. 카드별 정책과 현재 누적 사용 금액을 합쳐 실적 상태를 계산한다.
4. 새 결제를 반영했을 때 구간 달성 여부와 남은 금액 변화를 비교한다.
5. `BenefitRule`을 배타 그룹 단위로 평가하고, 규칙별 한도와 공유 한도를 순서대로 차감한다.
6. 실적 기여도와 예상 혜택을 함께 평가해 후보를 정렬한다.
7. 최상위 후보와 선택 사유, 대안을 반환한다.

## Boundary Notes

- `Money`는 공통 값 객체로 두고 금액 비교와 누적 계산의 기반으로 사용한다.
- `BenefitRule`은 카테고리뿐 아니라 `KB_PAY`, `ONLINE`, `OFFLINE`, `AUTO_BILL`, `SUBSCRIPTION`, `SIMPLE_PAY_ONLINE` 같은 거래 태그를 함께 본다.
- 한 거래에 대해 여러 혜택이 동시에 붙을 수 있으므로 `BenefitQuote`는 단일 규칙이 아니라 `AppliedBenefit` 목록을 가진다.
- 현재 시드 정책은 `My WE:SH KB국민카드(노는데 진심)`, `현대카드 ZERO Edition2(포인트형)`, `KB국민 노리2 체크카드(KB Pay)`, `K-패스 삼성카드`를 기준으로 구성한다.
- 발급월 유예, 전월실적 제외 항목, K-패스 정부 환급처럼 입력 데이터가 더 필요한 조건은 아직 부분 반영 상태다.
- 추천 사유 문자열은 API 표현이 아니라 도메인 판단 결과의 일부로 취급한다.
- 카드사 API 응답 모델은 core가 아니라 infra adapter에서만 해석한다.

## Known Gaps

- 카드 등록과 우선순위 조정은 타입과 포트는 존재하지만 아직 end-to-end 흐름으로 구현되지 않았다.
- 현재 도메인 baseline은 인메모리 정책과 fixture 데이터에 맞춰 설명 가능성을 우선한다.
- 영속화 이후에도 도메인 모델 자체는 유지하고, adapter만 교체하는 방향을 전제로 한다.
