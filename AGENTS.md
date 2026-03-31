# AGENTS.md

## Purpose

This repository is a Java/Spring backend demo for recommending which payment card to use based on monthly spending progress and benefit rules.

## Module Map

- `card-common`: shared value objects such as `Money`
- `card-core`: domain model, use cases, and ports
- `card-api`: Spring Boot app, REST controllers, normalization, demo fixtures, static UI
- `card-infra`: in-memory adapters for card catalog, card policies, and spending records

## Current Baseline

- The app is runnable as an in-memory demo, not just a design skeleton.
- The main flows currently implemented are:
  - card recommendation
  - monthly performance overview
  - spending record creation
  - card registration
  - card priority update
- Static demo UI is served by `card-api`.

## Important Implementation Rules

- Keep recommendation and performance logic in `card-core`.
- Keep Spring wiring, request normalization, demo fixtures, and HTTP concerns in `card-api`.
- Keep current persistence demo-oriented unless the task explicitly moves toward JPA/PostgreSQL.
- New card registration must also create a default card performance policy.
  - Reason: recommendation and performance overview assume every configured card has a policy.
  - Current default tier code is `DEFAULT_BASE`.
- Priority updates must include every configured card exactly once.
  - Partial reorder requests are rejected by design.

## In-Memory Adapter Notes

- `InMemoryCardCatalogAdapter` is mutable.
  - It supports load, save, and priority reorder.
- `InMemoryCardPerformancePolicyAdapter` is mutable.
  - It stores seeded policies plus the default policies created for newly registered cards.
- The current registration flow is safe for the demo because a newly added card gets a minimal default policy instead of breaking recommendation/performance endpoints.

## API Notes

- Card registration endpoint: `POST /api/cards`
- Card priority update endpoint: `PATCH /api/cards/priorities`
- Bad request handling is centralized in `card-api` via `ApiExceptionHandler`.

## Run And Verify

```bash
./gradlew test
./gradlew :card-api:bootRun
```

- Demo UI: `http://localhost:8080`

## Likely Next Work

- Add validation annotations and a more structured error payload if API hardening continues.
- Replace in-memory adapters with JPA/PostgreSQL adapters.
- Add explicit card policy management instead of relying on the default placeholder policy for newly registered cards.
