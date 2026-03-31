# Delivery Plan

## Current Milestone Status

- 인메모리 기반 추천 데모는 동작 가능한 상태다.
- 추천 API, 실적 조회 API, 사용 내역 입력 API와 정적 데모 UI가 구현돼 있다.
- 핵심 도메인 로직과 정규화 로직은 단위 테스트로 설명 가능하다.
- 다음 마일스톤의 핵심은 문서화가 아니라 미구현 유스케이스와 운영형 안정성 보강이다.

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
| 1 | API 안정성 보강 | validation, 에러 응답 형식, controller advice |
| 2 | 범위 미완성 구간 마감 | 카드 등록, 우선순위 조정 유스케이스와 엔드포인트 |
| 3 | 운영형 persistence 연결 | JPA/PostgreSQL 어댑터, 저장 구조, 통합 테스트 |
| 4 | 문서와 포트폴리오 마감 | API 문서, README 보강, 블로그용 정리 |

## Next Milestone Exit Criteria

- 카드 등록과 우선순위 조정 흐름이 end-to-end로 동작한다.
- 입력 검증과 일관된 예외 응답 형식이 추가된다.
- 카드/정책/사용 내역 저장이 JPA 기반으로 대체되거나, 인메모리 유지 사유가 문서화된다.
- 핵심 HTTP 흐름과 persistence 흐름을 통합 테스트로 검증한다.
- README와 docs 링크만 읽어도 현재 구현 상태와 다음 단계가 파악된다.
