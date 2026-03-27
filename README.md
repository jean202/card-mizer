# card-mizer

카드 실적을 추적하고 결제 시 어떤 카드를 우선 사용하는 게 좋은지 판단하기 위한 Java 백엔드 프로젝트입니다.

## 상태

- 현재 상태: 초기 구조 정리 중
- 목표: Spring Boot, 헥사고날 아키텍처, 멀티모듈 구조를 갖춘 백엔드 서비스
- 방향: 기능 구현보다 구조와 문서화를 먼저 정리하고 있습니다

## 현재 포함된 내용

- Gradle 멀티모듈 기본 골격
- `card-core`, `card-api`, `card-infra`, `card-common` 모듈 분리
- 아키텍처 및 모듈 구조 문서
- 초기 도메인/포트 인터페이스 뼈대

## 모듈

- `card-common`: 공통 타입과 예외를 둘 모듈
- `card-core`: 도메인과 유스케이스, 포트
- `card-api`: Spring Boot 기반 API 진입점
- `card-infra`: 저장소 및 외부 연동 어댑터

## 문서

- `docs/architecture.md`
- `docs/module-structure.md`
- `docs/adr/ADR-001-adopt-hexagonal-architecture.md`
- `PROJECT_PLAN.md`

## 메모

이 저장소는 공개용 초기 골격입니다. 실제 기능과 테스트, 실행 환경은 다음 단계에서 계속 추가할 예정입니다.
