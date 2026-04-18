# Delivery Plan

## Current Milestone Status

- JPA/H2 기반 추천 데모가 동작하며, PostgreSQL 프로파일과 compose.yaml도 준비돼 있다.
- 추천, 실적 조회, 사용 내역 입력, 카드 등록, 우선순위 조정, 카드 정책 조회/교체/부분 수정 API가 모두 구현돼 있다.
- 입력 검증과 구조화된 예외 응답이 적용돼 있다.
- springdoc-openapi + Scalar UI 기반 API 문서가 `/api-docs.html`에서 제공된다.
- GitHub Actions CI가 `./gradlew test`와 `:card-api:postgresIntegrationTest`를 자동 검증한다.
- 추천 API는 DB 기반 시나리오 end-to-end 테스트로 검증되고, PostgreSQL/Testcontainers 경로도 전용 태스크로 검증된다.
- 다음 마일스톤의 핵심은 설계 근거 문서화(ADR)와 운영 경로 구체화다.

## GitHub Projects Board

| 컬럼 | 의미 | 이동 기준 |
|------|------|-----------|
| `Backlog` | 아이디어와 후보 작업 | 아직 범위와 우선순위가 확정되지 않음 |
| `Ready` | 바로 착수 가능한 작업 | 요구사항과 완료 기준이 분명함 |
| `In Progress` | 현재 작업 중 | 한 사람 기준 동시 1~2개로 제한 |
| `Review` | 코드/문서 검토 대기 | PR 또는 설계 검토가 필요함 |
| `Done` | 완료된 작업 | 코드, 문서, 테스트 기준 충족 |

## Recommended Issue Types

- `design`: 도메인 모델, ADR, 구조 결정
- `domain`: 추천 규칙, 실적 계산, 정책 모델
- `api`: controller, DTO, validation
- `infra`: 인메모리/JPA/DB/외부 연동 어댑터
- `test`: 단위/통합/E2E 테스트
- `docs`: README, 블로그 연계 문서

## Next Work Queue

| 우선순위 | 목표 | 결과물 |
|------|------|--------|
| 1 | 설계 근거 문서화 | 추천 스코어링, 정책 JSON 직렬화 등 핵심 ADR 추가 |
| 2 | 운영용 PostgreSQL 경로 구체화 | 실행/배포 가이드, 환경 변수, compose 사용 경계 정리 |
| 3 | 다중 사용자 지원 설계 | 인증/인가 범위와 저장 모델 초안 |
| 4 | 실적 임박 알림 설계 | 임계치 계산, 이벤트, 스케줄링 방향 정리 |

## Next Milestone Exit Criteria

- 추천 스코어링과 정책 직렬화에 대한 ADR이 작성돼 있다.
- 운영용 PostgreSQL 실행 경로와 환경 변수 전략이 문서화돼 있다.
- 다중 사용자 지원을 위한 인증/인가 범위가 결정돼 있다.
- 실적 임박 알림의 진입점과 계산 책임이 문서로 정리돼 있다.
