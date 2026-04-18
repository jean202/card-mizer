# AGENTS.md

## Purpose

This repository is a Kotlin/Spring backend demo for recommending which payment card to use based on monthly spending progress and benefit rules.

## Module Map

- `card-common`: shared value objects such as `Money`
- `card-core`: domain model, use cases, and ports
- `card-api`: Spring Boot app, REST controllers, normalization, demo fixtures, static UI
- `card-infra`: JPA adapters for card catalog, card policies, and spending records

## Current Baseline

- The app is runnable as a Flyway + JPA/H2-backed demo, not just a design skeleton.
- The main flows currently implemented are:
  - card recommendation
  - monthly performance overview
  - spending record creation
  - card registration
  - card priority update
  - card performance policy read/replace/patch
- Static demo UI is served by `card-api`.

## Important Implementation Rules

- Keep recommendation and performance logic in `card-core`.
- Keep Spring wiring, request normalization, demo fixtures, and HTTP concerns in `card-api`.
- Keep current persistence demo-oriented even though it now runs through JPA/H2 by default.
- New card registration must also create a default card performance policy.
  - Reason: recommendation and performance overview assume every configured card has a policy.
  - Current default tier code is `DEFAULT_BASE`.
- Card policy management supports both full replacement and partial patch.
  - Endpoint shape: `GET/PUT/PATCH /api/cards/{cardId}/performance-policy`
  - `PUT` replaces the entire tier list and benefit rule list for that card.
  - `PATCH` keeps unspecified sections and only updates provided `tiers` and/or `benefitRules`.
- Priority updates must include every configured card exactly once.
  - Partial reorder requests are rejected by design.

## Persistence Notes

- The default runtime path now uses JPA adapters with H2.
- Schema is created through Flyway migrations under `card-api/src/main/resources/db/migration`.
- Default profile is `h2`; PostgreSQL is available through the `postgres` profile and env vars.
- Demo seed data is inserted on startup when the database is empty.
- `InMemory*Adapter` classes still exist as seed-data providers and lightweight reference implementations.
- Card performance policies are currently persisted as serialized JSON payloads behind the JPA adapter.
- The current registration flow is safe for the demo because a newly added card gets a minimal default policy instead of breaking recommendation/performance endpoints.

## API Notes

- Card registration endpoint: `POST /api/cards`
- Card priority update endpoint: `PATCH /api/cards/priorities`
- Card policy read endpoint: `GET /api/cards/{cardId}/performance-policy`
- Card policy replace endpoint: `PUT /api/cards/{cardId}/performance-policy`
- Card policy patch endpoint: `PATCH /api/cards/{cardId}/performance-policy`
- Bad request handling is centralized in `card-api` via `ApiExceptionHandler`.

## Run And Verify

```bash
./gradlew test
./gradlew :card-api:postgresIntegrationTest
./gradlew :card-api:bootRun
```

- Demo UI: `http://localhost:8080`

## Likely Next Work

- Add ADRs for recommendation scoring and policy JSON serialization decisions.
- Harden the production PostgreSQL runtime path and deployment guidance.
- Explore multi-user auth and threshold alert flows.
