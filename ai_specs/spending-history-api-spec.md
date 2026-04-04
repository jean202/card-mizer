<goal>
Add spending history query and soft-delete APIs to complete the spending record CRUD lifecycle.

Currently, spending records can only be created (POST) but never listed or removed. Users who manually
enter transactions or sync from card companies have no way to review what's been recorded or correct
mistakes. This feature closes that gap by adding GET (list with filters) and DELETE (soft delete)
endpoints, following the project's hexagonal architecture patterns.

**Who benefits:** Any user managing their card spending data — they can now review recorded transactions
and remove incorrect ones without losing audit history.
</goal>

<background>
**Tech stack:** Java 17, Spring Boot 3.4, Gradle multi-module, Hexagonal architecture
**Modules:** card-common (values), card-core (domain + ports), card-api (REST + assembly), card-infra (JPA adapters)
**Database:** H2 (default profile) / PostgreSQL (postgres profile), Flyway migrations

**Files to examine:**
- @card-core/src/main/java/com/jean202/cardmizer/core/domain/SpendingRecord.java — domain record
- @card-core/src/main/java/com/jean202/cardmizer/core/domain/SpendingPeriod.java — month-based period value
- @card-core/src/main/java/com/jean202/cardmizer/core/port/in/RecordSpendingUseCase.java — existing inbound port pattern
- @card-core/src/main/java/com/jean202/cardmizer/core/port/out/LoadSpendingRecordsPort.java — existing outbound port (loadByPeriod)
- @card-core/src/main/java/com/jean202/cardmizer/core/port/out/SaveSpendingRecordPort.java — existing save port
- @card-api/src/main/java/com/jean202/cardmizer/api/spending/SpendingRecordController.java — existing POST endpoint
- @card-api/src/main/java/com/jean202/cardmizer/api/config/ApplicationConfiguration.java — Spring bean assembly
- @card-infra/src/main/java/com/jean202/cardmizer/infra/persistence/jpa/JpaSpendingRecordAdapter.java — JPA adapter
- @card-infra/src/main/java/com/jean202/cardmizer/infra/persistence/jpa/JpaSpendingRecordRepository.java — Spring Data repo
- @card-infra/src/main/java/com/jean202/cardmizer/infra/persistence/jpa/JpaSpendingRecordEntity.java — JPA entity
- @card-infra/src/main/java/com/jean202/cardmizer/infra/persistence/InMemorySpendingRecordAdapter.java — in-memory adapter
- @card-api/src/main/resources/db/migration/V1__create_core_tables.sql — existing schema

**Existing patterns to follow:**
- Inbound ports are single-method interfaces in `core.port.in`
- Services implement ports and live in `core.application`
- Outbound ports are interfaces in `core.port.out`, implemented by JPA and InMemory adapters
- Bean wiring is explicit in `ApplicationConfiguration` (not component-scan on core)
- Controllers use `@Valid` request DTOs with Jakarta Validation
- Error responses use existing `ApiExceptionHandler`
</background>

<requirements>
**Functional:**
1. `GET /api/spending-records` returns spending records filtered by yearMonth and optionally cardId
2. `yearMonth` query param is optional; defaults to current month in Asia/Seoul (KST) timezone
3. `cardId` query param is optional; when provided, returns only records for that card
4. Response is wrapped: `{ "records": [...], "count": N, "yearMonth": "2026-03" }`
5. Each record in the response includes: id (UUID), cardId, amount (long, won), spentOn (yyyy-MM-dd), merchantName, merchantCategory, paymentTags (array of strings)
6. Records are sorted by spentOn descending (newest first), then by id descending for stable ordering. Sorting is done at the repository/adapter level (`ORDER BY spent_on DESC, id DESC`), consistent with the existing `findBySpentOnBetweenOrderBySpentOnAscIdAsc` pattern but reversed.
7. `DELETE /api/spending-records/{id}` soft-deletes a record by setting `deleted=true`
8. DELETE returns 204 No Content on success
9. Soft-deleted records are excluded from ALL read queries: spending list, performance overview, recommendations (sync has no dedup logic today — out of scope)

**Error Handling:**
10. DELETE returns 404 with structured error response if UUID not found or already deleted
11. GET with invalid yearMonth format (not YYYY-MM) returns 400 with validation message
12. GET with unknown cardId returns empty list (not 404) — consistent with performance-overview behavior

**Edge Cases:**
13. GET with no records for the period returns `{ "records": [], "count": 0, "yearMonth": "2026-03" }`
14. DELETE of an already-deleted record returns 404 (idempotent from user's perspective — "it doesn't exist")
15. Concurrent delete of same record: one succeeds (204), other gets 404

**Validation:**
16. yearMonth param must match YYYY-MM pattern when provided
17. cardId param must be non-blank when provided
18. DELETE path variable {id} must be a valid UUID
</requirements>

<boundaries>
**Edge cases:**
- Empty month: returns wrapped response with empty records array and count=0
- Unknown cardId filter: returns empty list, not error (card may have been registered after the queried month)
- Already-deleted record: DELETE returns 404 as if it never existed
- Very old months (e.g., 2020-01): works fine, returns whatever is stored
- Future months: allowed, returns empty (sync/manual entry might create future-dated records)

**Error scenarios:**
- Invalid UUID in DELETE path: 400 Bad Request (Spring default for type mismatch)
- Invalid yearMonth format: 400 with message "yearMonth must be in YYYY-MM format"
- Database unavailable: 500 (handled by existing Spring error handling)

**Limits:**
- No pagination — monthly spending records are naturally bounded (<200 per user/month typical)
- If pagination becomes needed later, it can be added as a backward-compatible enhancement
</boundaries>

<implementation>
**New files to create:**

card-core:
- `core/port/in/GetSpendingRecordsUseCase.java` — inbound port with method `getByPeriod(SpendingPeriod period, CardId cardId)` where cardId is nullable
- `core/port/in/DeleteSpendingRecordUseCase.java` — inbound port with method `delete(UUID id)`
- `core/application/GetSpendingRecordsService.java` — implements GetSpendingRecordsUseCase
- `core/application/DeleteSpendingRecordService.java` — implements DeleteSpendingRecordUseCase. This is intentionally a passthrough (delegates to `DeleteSpendingRecordPort.delete(id)`) for architectural consistency: every inbound port has a service, even trivial ones.
- `core/port/out/DeleteSpendingRecordPort.java` — outbound port: `void delete(UUID id)` throws `ResourceNotFoundException` if not found or already deleted. Domain says "delete"; adapter decides soft vs hard.
- `core/port/out/LoadSpendingRecordsByCardAndPeriodPort.java` — outbound port: `List<SpendingRecord> loadByPeriodAndCard(SpendingPeriod period, CardId cardId)` where cardId is nullable. When null, returns all records for the period. Sorted DESC (spentOn desc, id desc) — separate from existing `LoadSpendingRecordsPort` which sorts ASC and must not be modified.

card-api:
- None new; extend existing `SpendingRecordController`

card-infra:
- None new; extend existing adapters

**Files to modify:**

card-core:
- `core/port/out/LoadSpendingRecordsPort.java` — NO changes. Existing single-method interface stays as-is. Card-filtered loading uses the new `LoadSpendingRecordsByCardAndPeriodPort` instead.

card-api:
- `api/spending/SpendingRecordController.java` — add GET and DELETE endpoints with DTOs
- `api/config/ApplicationConfiguration.java` — wire new use case beans

card-infra:
- `infra/persistence/jpa/JpaSpendingRecordEntity.java` — add `deleted` boolean field
- `infra/persistence/jpa/JpaSpendingRecordRepository.java` — add queries with `deleted=false` filter + card filter
- `infra/persistence/jpa/JpaSpendingRecordAdapter.java` — implement new port methods, update existing `loadByPeriod` to exclude deleted
- `infra/persistence/InMemorySpendingRecordAdapter.java` — now implements 4 port interfaces. Track deleted IDs via a `Set<UUID> deletedIds` field. `delete(UUID)` adds to this set (throws `ResourceNotFoundException` if ID not in main list or already in deletedIds). All read methods (`loadByPeriod`, `loadByPeriodAndCard`) filter out IDs in `deletedIds`.

Database:
- `card-api/src/main/resources/db/migration/V3__add_spending_records_deleted_column.sql`

**Patterns to follow:**
- Use `SpendingPeriod` for month-based filtering (already exists)
- Use `CardId` value object for card identification (already exists)
- Inject `java.time.Clock` into `SpendingRecordController` for deterministic default-month resolution. `ApplicationConfiguration` wires `Clock.system(ZoneId.of("Asia/Seoul"))`; tests inject a fixed clock via `Clock.fixed(...)`
- Follow existing port/adapter separation strictly
- Wire beans in `ApplicationConfiguration`, not via `@Service` annotation on core classes
- Use `@Transactional(readOnly = true)` on read operations in JPA adapter

**What to avoid:**
- Do NOT add a `deleted` field to the `SpendingRecord` domain record — soft-delete is an infrastructure concern. The domain model should not know about it. The adapter handles filtering.
- Do NOT name port methods `softDelete` — the domain says "delete"; the adapter decides the mechanism (soft vs hard). This keeps core free of infrastructure leakage.
- Do NOT add `@Service` or Spring annotations to card-core classes
- Do NOT use Spring Data `@Query` for simple derived queries — use method-name-based query derivation
- Do NOT use `@Valid` on query params for yearMonth validation — parse manually via `YearMonth.parse()`. Invalid format throws `DateTimeParseException`, already mapped to 400 by `ApiExceptionHandler`
- Do NOT change the Flyway migration numbering if V3 already exists — check first
- The V3 migration and `JpaSpendingRecordEntity.deleted` field MUST be added in the same commit — `ddl-auto=validate` will reject mismatches at startup
</implementation>

<validation>
**Test strategy — TDD approach, behavior-first slices:**

**Unit tests (card-core) — RED → GREEN → REFACTOR per slice:**

1. `GetSpendingRecordsServiceTest`
   - Happy path: returns records for given period (uses InMemory adapter)
   - With cardId filter: returns only matching card's records
   - Without cardId: returns all cards' records for the period
   - Empty period: returns empty list
   - Verify sorting: spentOn desc, then id

2. `DeleteSpendingRecordServiceTest`
   - Happy path: delete existing record, verify it no longer appears in subsequent loads
   - Not found: adapter throws `ResourceNotFoundException` (already in `core.application`, already mapped to 404 by `ApiExceptionHandler`)
   - Already deleted: adapter throws `ResourceNotFoundException` (treated as not found)

**Integration tests (card-api) — controller layer:**

3. `SpendingRecordControllerTest` (extend existing)
   - GET with yearMonth returns matching records in wrapped format
   - GET with yearMonth + cardId returns filtered records
   - GET without params defaults to current month (KST)
   - GET with invalid yearMonth returns 400
   - GET for empty month returns `{ "records": [], "count": 0, ... }`
   - DELETE existing record returns 204
   - DELETE non-existent record returns 404
   - After DELETE, record excluded from GET results

4. `JpaPersistenceFlowIntegrationTest` (extend existing)
   - Verify soft-delete sets deleted=true in DB
   - Verify loadByPeriod excludes soft-deleted records
   - Verify loadByPeriodAndCard works correctly

**Testability seams:**
- `GetSpendingRecordsService` takes only `LoadSpendingRecordsByCardAndPeriodPort` via constructor (NOT `LoadSpendingRecordsPort` — that one sorts ASC and is used by recommendation/overview services). Use InMemory adapter in tests.
- `DeleteSpendingRecordService` takes `DeleteSpendingRecordPort` via constructor — adapter handles existence check internally, throws `ResourceNotFoundException` if not found
- InMemorySpendingRecordAdapter serves as the fake for all spending-related port tests
- Default month (KST timezone) should be injectable via `Clock` or similar for deterministic testing — pass `java.time.Clock` to controller or extract a `CurrentMonthResolver` that can be stubbed

**Mocking policy:**
- Prefer InMemory adapters (fakes) over mocks for port implementations
- Mock only if testing interaction patterns (e.g., verify `delete` was called)
- Controller tests use plain unit tests with hand-written capturing spies (existing pattern in SpendingRecordControllerTest) — no @WebMvcTest, no Spring context, direct constructor instantiation
</validation>

<done_when>
1. `GET /api/spending-records` returns wrapped response with records filtered by yearMonth (defaulting to current KST month) and optional cardId
2. `DELETE /api/spending-records/{id}` soft-deletes a record and returns 204
3. Soft-deleted records are invisible in spending list, performance overview, and recommendation queries
4. Flyway V3 migration adds `deleted` column with `default false` — existing data unaffected
5. All existing tests continue to pass unchanged
6. New unit tests cover GetSpendingRecordsService and DeleteSpendingRecordService (happy path + edge cases)
7. New/extended controller integration tests cover all endpoints and error cases
8. `./gradlew test` passes with zero failures
</done_when>
