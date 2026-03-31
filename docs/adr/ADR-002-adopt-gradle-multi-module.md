# ADR-002: Adopt Gradle Multi-Module Structure

## Status

Accepted

## Context

이 프로젝트는 도메인 모델, API 진입점, 영속성/외부 연동을 분리해 설명 가능해야 한다. 포트폴리오 관점에서도 "어떤 코드가 왜 core에 있고 왜 adapter에 있는지"가 저장소 구조에서 바로 보여야 한다.

## Decision

Gradle 멀티모듈 구조를 채택하고 다음 네 모듈을 기준선으로 삼는다.

- `card-common`
- `card-core`
- `card-api`
- `card-infra`

`card-core`는 공통 모듈을 제외한 다른 모듈에 의존하지 않는다.

## Consequences

- 도메인 코드와 프레임워크 코드를 분리해 설명하기 쉬워진다.
- 테스트 전략을 모듈별로 나눌 수 있다.
- 모듈 경계 설계가 필요해 초기 진입 비용은 높아진다.
- 작은 기능 추가에도 경계 판단이 필요하므로 설계 규율이 요구된다.
