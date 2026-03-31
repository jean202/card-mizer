# ADR-005: Store Card Performance Policy as JSON

## Status

Accepted

## Context

`CardPerformancePolicy`는 카드별 실적 구간(tiers)과 혜택 규칙(benefitRules)을 담는다. 혜택 규칙 하나에 19개 이상의 필드가 있고, 배타 그룹, 공유 한도, 월별 상한 구간(tiered caps) 같은 중첩 구조도 포함한다.

이 데이터를 저장하는 방식은 크게 두 가지다.

1. **정규화된 테이블**: tiers, benefit_rules, monthly_cap_tiers, merchant_categories 등 5~7개 테이블로 분리
2. **JSON 직렬화**: 카드당 한 행에 tiers_json, benefit_rules_json 두 컬럼으로 저장

## Decision

카드 정책은 카드 ID를 기준으로 한 행에 저장하고, 실적 구간과 혜택 규칙은 각각 JSON TEXT 컬럼으로 직렬화한다.

```
card_performance_policies (
    card_id       VARCHAR(100) PK,
    tiers_json    TEXT,
    benefit_rules_json TEXT
)
```

직렬화/역직렬화는 `card-infra`의 JPA 어댑터가 Jackson ObjectMapper로 처리한다.

## Consequences

- 혜택 규칙에 새 필드가 추가돼도 스키마 마이그레이션 없이 JSON 구조만 확장하면 된다. 실제로 `sharedMonthlyCapTiers`, `sharedYearlyBenefitCap` 같은 필드가 설계 중에 추가됐고, 테이블 변경 없이 반영됐다.
- "카드 X의 정책 전체를 가져온다"는 조회 패턴에 최적화돼 있다. 정규화된 설계에서는 5~7개 테이블을 JOIN해야 하는 쿼리가 단일 행 조회로 끝난다.
- "특정 가맹점 카테고리에 해당하는 규칙이 있는 카드 목록"처럼 규칙 내부를 기준으로 검색하는 쿼리는 JSON 파싱이 필요해 비효율적이다. 현재 코드는 전체 정책을 로드한 뒤 애플리케이션에서 필터링한다.
- 정책 교체(PUT)와 부분 수정(PATCH) 모두 전체 JSON을 덮어쓰는 방식이다. 규칙 하나만 바꿔도 전체 JSON이 갱신되므로 동시 수정 충돌 시 마지막 쓰기가 이긴다.
- PostgreSQL로 전환하면 TEXT 대신 JSONB 타입을 사용해 인덱싱과 부분 쿼리를 지원할 수 있다. 현재는 이 최적화를 미루고 있다.
- tiers와 benefitRules를 두 컬럼으로 분리한 이유는 도메인 개념의 구분(실적 구간은 목표, 혜택 규칙은 보상)을 저장 구조에도 반영하기 위함이다.
