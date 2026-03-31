# Delivery Plan

## Current Milestone Status

- JPA/H2 기반 추천 데모가 동작하며, PostgreSQL 프로파일과 compose.yaml도 준비돼 있다.
- 추천, 실적 조회, 사용 내역 입력, 카드 등록, 우선순위 조정, 카드 정책 조회/교체/부분 수정 API가 모두 구현돼 있다.
- 입력 검증과 구조화된 예외 응답이 적용돼 있다.
- springdoc-openapi + Scalar UI 기반 API 문서가 `/api-docs.html`에서 제공된다.
- 핵심 도메인 로직, 컨트롤러, persistence 흐름은 단위 테스트와 통합 테스트로 검증된다.
- 다음 마일스톤의 핵심은 설계 근거 문서화(ADR)와 테스트 보강이다.

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
| 2 | PostgreSQL 통합 테스트 | Testcontainers 기반 마이그레이션 + 주요 흐름 검증 |
| 3 | 추천 E2E 테스트 보강 | DB 기반 시나리오 테스트로 추천 흐름 end-to-end 검증 |
| 4 | 문서 최종 점검 | README, 설계 문서, API 문서 링크 정리 |

## Next Milestone Exit Criteria

- 추천 스코어링과 정책 직렬화에 대한 ADR이 작성돼 있다.
- PostgreSQL 환경에서 Flyway 마이그레이션과 주요 API 흐름이 통합 테스트로 검증된다.
- 추천 API가 DB 기반 end-to-end 시나리오 테스트로 검증된다.
- README와 docs 링크만 읽어도 현재 구현 상태와 다음 단계가 파악된다.
