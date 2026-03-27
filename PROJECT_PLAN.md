# card-mizer — 카드 실적 관리 & 결제 추천 서비스

## 프로젝트 개요

실제 사용하는 카드(삼성, 국민 신용/체크, 현대) 4장의 실적을 추적하고, 결제 시 어떤 카드를 써야 최적인지 추천하는 백엔드 서비스.

### 목적
- **포트폴리오 프로젝트**: Java/Spring 기반 헥사고날 아키텍처 + Gradle 멀티모듈 역량 증명
- **실사용 목적**: 본인의 카드 실적 관리 자동화

---

## 기술 스택

| 항목 | 기술 |
|------|------|
| Language | Java 17+ |
| Framework | Spring Boot 3 |
| ORM | JPA + QueryDSL |
| DB | PostgreSQL |
| Build | Gradle 멀티모듈 |
| Test | JUnit 5, Testcontainers |
| CI/CD | GitHub Actions |
| 문서화 | Spring REST Docs |
| 아키텍처 | Hexagonal (Ports & Adapters) |

---

## 아키텍처: 헥사고날 (Ports & Adapters)

### 왜 헥사고날인가

2005년 Alistair Cockburn이 제안. Hex = 6, Hexagon = 6각형. 애플리케이션을 육각형 다이어그램으로 그려서 붙은 별명이며, 정식 명칭은 "Ports and Adapters 패턴".

기존 3계층 레이어(Controller → Service → Repository)를 위→아래로 쌓으면 '위=입력, 아래=출력'이라는 고정관념이 생김. 육각형으로 그리면 **어느 방향에서든 들어오고 나갈 수 있다**는 걸 직관적으로 보여줌.

```
         ┌─────────────┐
        ╱   REST API    ╲
       ╱                 ╲
      │    ┌─────────┐    │
 CLI ─│───▶│  Domain  │◀───│── Kafka / Scheduler
      │    │  (Core)  │    │
       ╲   └─────────┘   ╱
        ╲   DB Adapter  ╱
         └─────────────┘
```

### 이 프로젝트에 적합한 이유

| 헥사고날 조건 | 이 프로젝트에서의 충족 |
|--------------|----------------------|
| 외부 시스템 연동 2+ | 카드사별 API(삼성/국민/현대), 알림(Slack/카카오) |
| 복잡한 도메인 규칙 | 카드별 다단계 구간 실적, 우선순위 전략, 결제 추천 알고리즘 |
| 진입점 2+ | REST API + 스케줄러(월초 리셋, 실적 마감 임박 알림) |
| 모듈 자연 분리 | 도메인(실적 계산) / API / 인프라(카드사 연동, 알림) |
| core 프레임워크 독립 | 실적 계산/추천 로직은 순수 Java로 구현 |

### 핵심 원칙
- **도메인 코어가 외부에 의존하지 않음** — Port(인터페이스)와 Adapter(구현체)로만 연결
- **core 모듈에 Spring/JPA 의존성 없음** — 순수 Java 단위 테스트 가능
- **Adapter 교체가 도메인에 영향 없음** — 수동입력 → 카드사 API로 전환 시 Adapter만 변경

---

## 멀티모듈 구조

```
card-mizer/
│
├── card-core           # 순수 Java, 프레임워크 의존 없음
│   ├── domain/
│   │   ├── Card              (카드 정보, 구간 정책)
│   │   ├── SpendingRecord    (사용 내역)
│   │   ├── PerformanceTier   (실적 구간 상태)
│   │   ├── Priority          (우선순위 전략)
│   │   └── PaymentRecommendation (추천 결과)
│   ├── usecase/
│   │   ├── RecommendCardUseCase     ← "이 금액, 어떤 카드?"
│   │   ├── TrackSpendingUseCase     ← "결제 기록 반영"
│   │   └── CheckPerformanceUseCase  ← "현재 실적 현황"
│   └── port/
│       ├── in/   (Inbound - UseCase 인터페이스)
│       └── out/  (Outbound - 저장소, 외부 연동 인터페이스)
│
├── card-api            # Spring Boot, Controller, DTO
│   ├── REST API 엔드포인트
│   └── 요청/응답 DTO 변환
│
├── card-infra          # JPA, 외부 API, 알림
│   ├── persistence/    ← JPA Adapter (PostgreSQL)
│   ├── cardcompany/    ← 카드사 API Adapter (삼성, 국민, 현대)
│   └── notification/   ← 알림 Adapter (Slack, 카카오톡)
│
├── card-common         # 공통 예외, 유틸
│
└── docs/               # 아키텍처 문서
    ├── architecture.md
    ├── module-structure.md
    └── decision-log.md  (ADR)
```

### 모듈 의존 방향 (안쪽으로만)

```
card-api  ──→  card-core  ←──  card-infra
                 ↑
            card-common (모든 모듈 참조)
```

---

## 핵심 기능

### 1. 실적 대시보드 — "지금 내 카드 상태가 어때?"

```
삼성카드    ████████░░░░  28만/40만 (1구간) — 12만원 남음
국민 신용   ██████████████  달성 ✓
국민 체크   ███░░░░░░░░░  6만/20만 — 14만원 남음
현대카드    ░░░░░░░░░░░░  0만/30만 — 비활성 (우선순위 5)
```

### 2. 결제 추천 — "15만원 쓸 건데 어디로?"

```
POST /api/recommend
{ "amount": 150000, "merchant": "쿠팡" }

→ 추천: 삼성카드
  - 사유: 1구간 달성까지 12만원, 이 결제로 달성 + 잔여 3만원 2구간 반영
  - 대안: 현대카드 (쿠팡 2% 적립, 하지만 실적 우선순위 낮음)
```

### 3. 우선순위 관리 — "이번 달은 순서 바꿀래"

```
PUT /api/priority
[ "KB_CREDIT", "SAMSUNG_T1", "SAMSUNG_T2", "KB_CHECK", "HYUNDAI" ]
```

---

## 헥사고날 어필 포인트: Adapter 교체 시연

```java
// Port (core 모듈 — 인터페이스만)
public interface CardCompanyPort {
    SpendingSummary fetchMonthlySpending(CardInfo card, YearMonth month);
}

// Adapter A: 수동 입력 (infra 모듈) — 초기 구현
public class ManualInputAdapter implements CardCompanyPort { ... }

// Adapter B: 실제 카드사 API (infra 모듈) — 이후 교체
public class SamsungCardApiAdapter implements CardCompanyPort { ... }

// Adapter C: 테스트용 가짜 구현
public class FakeCardCompanyAdapter implements CardCompanyPort { ... }
```

→ 처음엔 수동 입력으로 시작, 카드사 API 열리면 Adapter만 교체 — 헥사고날의 존재 이유를 실전으로 증명

---

## 도메인 규칙 예시

```java
// 단순 CRUD가 아닌, 도메인 로직이 객체 안에서 판단/계산
public PaymentRecommendation recommend(int amount, String merchant) {
    // 1. 우선순위 순으로 카드 순회
    // 2. 각 카드의 현재 실적 구간 확인
    // 3. 이 결제로 구간 달성 가능 여부 판단
    // 4. 가맹점별 혜택 비교 (실적 달성 vs 즉시 할인)
    // 5. 최적 카드 + 사유 반환
}
```

---

## 테스트 전략

| 레이어 | 테스트 방식 | 도구 |
|--------|-----------|------|
| core (도메인) | 순수 단위 테스트, 외부 의존 없음 | JUnit 5만 |
| infra (어댑터) | 통합 테스트, 실제 DB | Testcontainers |
| api (컨트롤러) | 슬라이스 테스트 | MockMvc / WebTestClient |
| 전체 | E2E 시나리오 | SpringBootTest + Testcontainers |

---

## GitHub 활용 계획

- **GitHub Projects (칸반)**: 학습 → 설계 → 구현 → 테스트 단계 관리
- **docs/ ADR**: 아키텍처 결정 근거 문서화
- **GitHub Wiki**: 헥사고날/DDD 학습 노트
- **GitHub Actions**: CI/CD 파이프라인

---

## 다른 포트폴리오 프로젝트와의 연계

이 프로젝트는 3개 Java 포트폴리오 프로젝트 중 하나:

| 프로젝트 | 증명하는 역량 |
|----------|-------------|
| **card-mizer (이것)** | 아키텍처 설계, 도메인 모델링, 테스트 전략 |
| Streamgate (실시간 파이프라인) | 리액티브(WebFlux), 메시징(Kafka), 고성능 처리 |
| hook-notify (오픈소스 SDK) | API/인터페이스 설계, 라이브러리 배포 |

hook-notify SDK를 이 프로젝트의 알림 모듈에서 실제 사용 예정 → "내가 만든 라이브러리를 내 프로젝트에서 쓴다"는 스토리

---

## 새 세션에서 시작할 때 프롬프트

아래 프롬프트를 새 세션에 붙여넣으면 바로 이어서 작업 가능:

```
/Users/admin/IdeaProjects/card-mizer/PROJECT_PLAN.md 파일을 읽고,
이 계획에 따라 프로젝트 초기 세팅을 해줘.

작업 범위:
1. Gradle 멀티모듈 프로젝트 구조 생성 (card-core, card-api, card-infra, card-common)
2. 모듈 간 의존성 설정 (core는 프레임워크 독립)
3. docs/ 디렉토리에 architecture.md, module-structure.md, decision-log.md(ADR-001) 생성
4. GitHub 레포 생성 및 초기 커밋
5. GitHub Projects 칸반 보드 생성

참고:
- 목업/더미 데이터 금지, 실제 동작하는 코드로 작성
- 요청한 작업만 수행, 주변 코드 건드리지 않기
```
