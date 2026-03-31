# ADR-001: Adopt Hexagonal Architecture

## Status

Accepted

## Context

이 프로젝트는 카드별 실적 정책, 우선순위 전략, 결제 추천 규칙처럼 도메인 판단이 중심이다. 현재 기준선은 수동 입력, 가맹점 정규화, 추천 시나리오 fixture, 인메모리 persistence adapter를 포함한 실행 가능한 데모다. 이후 카드사 연동, 알림, 스케줄러, JPA persistence가 자연스럽게 추가될 가능성이 높다.

## Decision

도메인 코어를 포트 기반으로 정의하고, REST API와 영속성, 외부 연동은 어댑터로 분리한다. `card-core`는 프레임워크에 직접 의존하지 않는 순수 Java 모듈로 유지한다. 현재 기준선에서는 REST API, 정적 데모 UI, 정규화 규칙 로더, 인메모리 persistence adapter가 모두 이 경계 위에서 동작한다.

## Consequences

- 도메인 로직 테스트가 쉬워진다.
- 저장소나 외부 연동 변경이 코어에 덜 영향을 준다.
- 같은 유스케이스를 REST API 외의 진입점에서도 재사용할 수 있다.
- 현재처럼 인메모리 adapter로 시작하고 이후 JPA adapter로 교체하는 흐름을 코어 변경 없이 설명할 수 있다.
- `card-api`에 데모 시연 책임이 일부 함께 있어도 핵심 도메인 규칙은 `card-core`에 남겨 둘 수 있다.
- 구조 설계 비용은 늘어나지만, 포트폴리오와 장기 유지보수 관점에서는 이점이 크다.
