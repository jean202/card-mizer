# Project Plan

## Current Decision Snapshot

- 프로젝트 이름은 `card-mizer`를 유지한다.
- 한 줄 소개는 "카드 실적과 혜택 우선순위를 계산해 결제 카드를 추천하는 Java 백엔드"로 고정한다.
- 이 저장소는 블로그 저장소와 분리된 대표 포트폴리오 프로젝트다.
- 현재 기준선은 인메모리 어댑터와 정적 데모 UI를 포함한 실행 가능한 백엔드 데모다.
- `card-core`는 계속 순수 Java 도메인 모듈로 유지하고, 조립과 시연용 요소는 `card-api`에 둔다.
- JPA/PostgreSQL 같은 운영형 인프라는 다음 단계에서 붙이고, 먼저 미구현 유스케이스와 입력 안정성을 보강한다.

## Portfolio Position

`card-mizer`는 GitHub 프로필에서 가장 먼저 보여줄 대표 Java 프로젝트를 목표로 한다. 핵심 메시지는 "Spring 기반의 실사용 문제를 도메인 로직과 아키텍처 설계로 풀어낸 백엔드"다.

## Design Docs Index

- `docs/product-overview.md`
- `docs/domain-model.md`
- `docs/architecture.md`
- `docs/module-structure.md`
- `docs/adr/README.md`
- `docs/delivery-plan.md`

## Current Build State

- 추천, 실적 조회, 사용 내역 입력 API가 구현돼 있다.
- 가맹점 정규화 규칙과 추천 시나리오 fixture가 YAML로 관리된다.
- Spring Boot 정적 리소스로 데모 화면을 함께 제공한다.
- 카드 카탈로그, 카드 정책, 사용 내역 저장은 현재 인메모리 어댑터로 동작한다.
- `./gradlew test` 기준 핵심 도메인/컨트롤러 단위 테스트가 통과한다.

## Immediate Priorities

1. README와 설계 문서를 현재 코드 상태에 맞게 유지한다.
2. 입력 검증과 예외 응답 형식을 추가해 API 안정성을 높인다.
3. 카드 등록과 우선순위 조정 유스케이스를 구현한다.
4. JPA/PostgreSQL 어댑터와 통합 테스트를 붙인다.
5. API 문서와 블로그용 설계 근거를 정리한다.

## Scope Boundary

포함:

- 월별 사용 내역 수동 입력
- 현재 실적 현황 조회
- 결제 금액 기준 추천 카드 계산
- 가맹점/결제 태그 정규화
- 백엔드 시연용 정적 데모 UI

아직 미구현:

- 카드 등록
- 카드별 실적 정책 등록/관리
- 카드 우선순위 조정

제외:

- 카드사 API 연동
- 푸시/메신저 알림
- 별도 프론트엔드 애플리케이션
- 복수 사용자 인증/권한
- 고급 혜택 최적화 엔진
