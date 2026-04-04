## Overview

GET + soft-DELETE for spending records. Hexagonal 4-module approach: ports → services → adapters → controller, with V3 Flyway migration for `deleted` column.

**Spec**: `ai_specs/spending-history-api-spec.md` (read this file for full requirements)

## Context

- **Structure**: Hexagonal, 4 Gradle modules (card-common, card-core, card-api, card-infra)
- **State management**: Spring Boot 3.4, explicit bean wiring in `ApplicationConfiguration`
- **Reference implementations**:
  - `card-api/.../spending/SpendingRecordController.java` — existing POST endpoint
  - `card-core/.../application/GetPerformanceOverviewService.java` — service with port injection pattern
  - `card-infra/.../jpa/JpaSpendingRecordAdapter.java` — multi-port JPA adapter
  - `card-api/.../config/ApplicationConfiguration.java` — bean wiring
- **Conventions verified**:
  - Inbound ports: single-method interfaces, no annotations
  - Services: constructor injection via `Objects.requireNonNull`
  - Outbound ports: single-method, Load*/Save*/Delete* naming
  - JPA adapters: `@Component @Primary @Transactional`, implements multiple ports
  - Entities: no Lombok, protected no-arg + full constructor, getters only
  - Repositories: method-name derivation only, no `@Query`
  - Controller tests: plain JUnit 5 with hand-written capturing spies
  - Core service tests: inline lambda port fakes
  - Integration tests: `@SpringBootTest` + MockMvc + jsonPath
- **Assumptions/Gaps**: None — spec refined through 3 review rounds

## Plan

### Phase 1: Core ports + InMemory adapter + service tests (vertical slice)

- **Goal**: Domain layer complete with passing tests before touching API/JPA
- [x] `card-core/.../core/port/out/LoadSpendingRecordsByCardAndPeriodPort.java` — `List<SpendingRecord> loadByPeriodAndCard(SpendingPeriod period, CardId cardId)`. CardId nullable; sorted DESC. Single method.
- [x] `card-core/.../core/port/out/DeleteSpendingRecordPort.java` — `void delete(UUID id)`. Throws `ResourceNotFoundException` if not found/already deleted.
- [x] `card-core/.../core/port/in/GetSpendingRecordsUseCase.java` — `List<SpendingRecord> getByPeriod(SpendingPeriod period, CardId cardId)`. CardId nullable.
- [x] `card-core/.../core/port/in/DeleteSpendingRecordUseCase.java` — `void delete(UUID id)`.
- [x] `card-core/.../core/application/GetSpendingRecordsService.java` — implements `GetSpendingRecordsUseCase`. Depends on `LoadSpendingRecordsByCardAndPeriodPort` only. Constructor validates with `Objects.requireNonNull`.
- [x] `card-core/.../core/application/DeleteSpendingRecordService.java` — implements `DeleteSpendingRecordUseCase`. Passthrough to `DeleteSpendingRecordPort.delete(id)`. Constructor validates.
- [x] `card-infra/.../persistence/InMemorySpendingRecordAdapter.java` — extend to implement `LoadSpendingRecordsByCardAndPeriodPort` + `DeleteSpendingRecordPort`. Add `Set<UUID> deletedIds`. `delete()` throws `ResourceNotFoundException` if ID unknown or already deleted. All reads filter deletedIds. `loadByPeriodAndCard` sorts DESC + filters by cardId when non-null.
- [x] TDD: `GetSpendingRecordsServiceTest` — happy path returns records for period via inline lambda port fake
- [x] TDD: `GetSpendingRecordsServiceTest` — with cardId filter returns only matching card's records
- [x] TDD: `GetSpendingRecordsServiceTest` — without cardId returns all cards' records
- [x] TDD: `GetSpendingRecordsServiceTest` — empty period returns empty list
- [x] TDD: `GetSpendingRecordsServiceTest` — verify sorting spentOn desc then id desc
- [x] TDD: `DeleteSpendingRecordServiceTest` — delete existing record delegates to port
- [x] TDD: `DeleteSpendingRecordServiceTest` — not found throws `ResourceNotFoundException`
- [x] TDD: `DeleteSpendingRecordServiceTest` — already deleted throws `ResourceNotFoundException`
- [x] Verify: `./gradlew :card-core:test` && `./gradlew :card-infra:test`

### Phase 2: Flyway migration + JPA entity + JPA adapter + existing queries exclude deleted

- **Goal**: Persistence layer supports soft-delete; existing features unaffected
- [x] `card-api/.../resources/db/migration/V3__add_spending_records_deleted_column.sql` — `ALTER TABLE spending_records ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;`
- [x] `card-infra/.../jpa/JpaSpendingRecordEntity.java` — add `private boolean deleted;` field with `@Column(nullable = false)`. Add to full constructor + getter + setter.
- [x] `card-infra/.../jpa/JpaSpendingRecordRepository.java` — add derived queries with `deletedFalse` filter
- [x] `card-infra/.../jpa/JpaSpendingRecordAdapter.java` — implement 4 ports, soft-delete via findById+setDeleted+save
- [x] Update `toEntity` mapper to include `deleted` field (always `false` for new records)
- [x] Verify: `./gradlew test` — ALL existing tests pass

### Phase 3: Controller endpoints + bean wiring + controller tests

- **Goal**: API layer complete; full feature working end-to-end
- [ ] `card-api/.../config/ApplicationConfiguration.java` — add `Clock` bean: `Clock.system(ZoneId.of("Asia/Seoul"))`. Add `GetSpendingRecordsUseCase` bean (wires `LoadSpendingRecordsByCardAndPeriodPort`). Add `DeleteSpendingRecordUseCase` bean (wires `DeleteSpendingRecordPort`).
- [ ] `card-api/.../spending/SpendingRecordController.java` — add `Clock` + use case dependencies to constructor. Add:
  - `GET /api/spending-records` — `@GetMapping` with `@RequestParam(required=false) String yearMonth` and `@RequestParam(required=false) String cardId`. Parse yearMonth via `YearMonth.parse()` (defaults to current KST month via Clock). Returns `SpendingRecordsResponse` wrapper record.
  - `DELETE /api/spending-records/{id}` — `@DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)` with `@PathVariable UUID id`. Delegates to use case.
  - Response DTO: `SpendingRecordsResponse(List<SpendingRecordResponse> records, int count, String yearMonth)` record.
  - Item DTO: `SpendingRecordResponse(UUID id, String cardId, long amount, LocalDate spentOn, String merchantName, String merchantCategory, Set<String> paymentTags)` record with `static from(SpendingRecord)` factory.
- [ ] TDD: `SpendingRecordControllerTest` — GET with yearMonth returns wrapped response (capturing spy for `GetSpendingRecordsUseCase`, fixed `Clock`)
- [ ] TDD: `SpendingRecordControllerTest` — GET with yearMonth + cardId passes CardId to use case
- [ ] TDD: `SpendingRecordControllerTest` — GET without params defaults to current KST month
- [ ] TDD: `SpendingRecordControllerTest` — DELETE calls use case with correct UUID
- [ ] TDD: extend `JpaPersistenceFlowIntegrationTest` — POST spending → DELETE → verify GET excludes it; verify loadByPeriod excludes deleted
- [ ] Verify: `./gradlew test` — zero failures

## Risks / Out of scope

- **Risks**:
  - Derived query method names are long (`findByDeletedFalseAndCardIdAndSpentOnBetween...`); verify Spring Data parses correctly on first run
  - `InMemorySpendingRecordAdapter` now implements 4 ports — if it becomes unwieldy, split later
- **Out of scope**:
  - Pagination (deferred; monthly records bounded <200)
  - Sync dedup (no dedup logic exists today)
  - Demo UI integration (static HTML not updated)
  - Bulk delete
