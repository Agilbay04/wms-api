# Warehouse Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `dot-plugin:subagent-driven-development` or `dot-plugin:executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Issue:** Warehouse-management training project defined in `docs/erd/wms.dbml`, `docs/use_case/warehouse-management.md`, and `docs/silabus/java-and-springboot.md`.

**Goal:** Deliver a secure, transactional Warehouse Management REST API and use each module to learn Java 17 and Spring Boot.

**Architecture:** A package-by-feature modular monolith under `com.warehousing.wmsapi`. Each feature has HTTP, application, and persistence classes; shared concerns remain in `common`. PostgreSQL is the source of truth, Redis holds revocations, caches, and Streams, and Mailpit receives development email.

**Tech stack:** Java 17, Gradle, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, Spring Security, PostgreSQL 15, Flyway, Redis 7, Spring Mail, springdoc, JUnit 5, and Mockito.

## File structure

| Area | Responsibility |
| --- | --- |
| `src/main/resources/application*.properties` | Profiled, environment-backed configuration. |
| `src/main/resources/db/migration` | Ordered Flyway schema and seed migrations. |
| `src/main/java/.../common` | Envelope, errors, pagination, auditing, and shared JPA base classes. |
| `src/main/java/.../auth` | JWT login, refresh, logout, and Security configuration. |
| `src/main/java/.../{iam,master,inbound,inventory,outbound,reporting}` | One bounded context per business capability. |
| `src/test/java/...` | Unit tests plus opt-in HTTP integration tests against a dedicated HomeServer test environment. |
| `docs/openapi` and `docs/postman` | API contract and executable client collection. |

## Tasks

### Task 1: Make the project reproducible

**Description:** Add required dependencies and profiles without committing credentials.

**Skill:** `backend-development`

**Files:**
- Modify: `build.gradle`, `src/main/resources/application.properties`
- Create: `src/main/resources/application.properties`, `src/main/resources/application-dev.properties`, `src/main/resources/application-stg.properties`, `src/main/resources/application-prod.properties`, `.env.example`, `.gitignore`

- [ ] Add Flyway, Spring Security, Spring Mail, JWT, and cache dependencies to `build.gradle`; keep Java toolchain at 17.
- [ ] Replace the single properties file with YAML. Bind these required environment variables: `WMS_DB_HOST`, `WMS_DB_PORT`, `WMS_DB_NAME`, `WMS_DB_USERNAME`, `WMS_DB_PASSWORD`, `WMS_REDIS_HOST`, `WMS_REDIS_PORT`, `WMS_REDIS_PASSWORD`, `WMS_MAIL_HOST`, `WMS_MAIL_PORT`, `WMS_MAIL_USERNAME`, `WMS_MAIL_PASSWORD`, `WMS_JWT_SECRET`, and the three seed-admin variables.
- [ ] Set safe development defaults only for host and port: PostgreSQL `localhost:5432`, Redis `localhost:6379`, and Mailpit SMTP `localhost:1025`; leave all credentials blank in `.env.example`.
- [ ] Set JPA `ddl-auto: validate`, enable Flyway, configure Hikari timeouts, disable SQL logging by default, and configure Mailpit from environment variables.
- [ ] Add `.env` and generated `exports/` to `.gitignore`.
- [ ] Run `./gradlew test`.

**Verification:** The context test compiles, and the application reports a clear missing-environment error instead of using a credential embedded in source.

### Task 2: Create the database contract and migrations

**Description:** Translate the ERD into checked-in Flyway SQL before writing entities.

**Skill:** `design-dbml-database`

**Files:**
- Modify: `docs/erd/wms.dbml`
- Create: `src/main/resources/db/migration/V1__create_iam.sql` through `V8__add_optimistic_lock_columns.sql`
- Test: `src/test/java/com/warehousing/wmsapi/persistence/FlywayMigrationIntegrationTest.java`

- [ ] Review the DBML and keep UUID primary keys, soft-delete timestamps, unique constraints, and all foreign-key indexes.
- [ ] Write ordered migrations for IAM, master data, inbound/outbound, transfer/adjustment, location balances, movements, and audit trails. Use `timestamptz`, `uuid`, `check (quantity >= 0)` for balance rows, and `(warehouse_location_id, product_id)` as the unique balance key.
- [ ] Add indexes matching list queries: warehouse/status/created time, product/location/occurred time, and entity type/entity ID for audits.
- [ ] Write an opt-in migration integration test that connects to the dedicated HomeServer `wms_test` PostgreSQL database, runs Flyway, and asserts that `users`, `warehouse_location_items`, `stock_movements`, and `audit_trails` exist.
- [ ] Run `./gradlew integrationTest --tests '*FlywayMigrationIntegrationTest'` only after `WMS_TEST_DB_*` credentials are supplied.

**Verification:** A blank dedicated HomeServer test database migrates successfully and Hibernate validates the final schema.

### Task 3: Build common HTTP and persistence foundations

**Description:** Establish the conventions all features use.

**Skill:** `backend-development`

**Files:**
- Create: `common/api/ApiResponse.java`, `common/api/PageResponse.java`, `common/error/ApiExceptionHandler.java`, `common/error/BusinessException.java`, `common/persistence/AuditableEntity.java`, `common/pagination/PageRequest.java`
- Test: `common/error/ApiExceptionHandlerTest.java`

- [ ] Implement a generic response envelope with numeric HTTP status in `code`, message, data, and field-error map.
- [ ] Implement `@RestControllerAdvice` mappings: malformed request 400, unauthenticated 401, unauthorized 403, absent data 404, duplicate/version conflict 409, business validation 422, and unexpected errors 500 without a stack trace in the response.
- [ ] Implement `AuditableEntity` with UUID ID, created/updated/deleted timestamps, and optimistic `@Version`; use it only for mutable domain entities.
- [ ] Implement page-size validation from 1 to 100 and response metadata `page`, `size`, `totalElements`, and `totalPages`.
- [ ] Test validation and error-envelope JSON through MockMvc.

**Verification:** Every controlled error has the same JSON shape and no internal database detail.

### Task 4: Implement IAM, JWT, and warehouse authorization

**Description:** Secure every business endpoint before exposing domain data.

**Skill:** `backend-development`

**Files:**
- Create: `auth/{controller,dto,security,service}/*`, `iam/{entity,repository,service,controller,dto}/*`, `common/security/CurrentUser.java`, `common/security/WarehouseAccessService.java`
- Create: `config/DevDataSeeder.java`
- Test: `auth/AuthIntegrationTest.java`, `iam/WarehouseAccessIntegrationTest.java`

- [ ] Model users, roles, permissions, user roles, role permissions, and user warehouses. Map password as `passwordHash` in Java while retaining the ERD column name through `@Column(name = "password")`.
- [ ] Configure BCrypt, a stateless security filter chain, method security, JWT access tokens, and opaque refresh tokens. Store the refresh-token hash and revoked-token identifier in Redis with their TTL.
- [ ] Implement `POST /api/v1/auth/login`, `/refresh`, and `/logout`; issue 15-minute access tokens and rotate refresh tokens.
- [ ] Implement `hasPermission(resource, operation)` and `canAccessWarehouse(warehouseId)` checks. Apply both to every protected command and query, not only to list filters.
- [ ] Implement `DevDataSeeder` as an `ApplicationRunner` annotated with `@Profile("dev")`. It reads the three `WMS_SEED_ADMIN_*` values, BCrypt-hashes the password, and inserts RBAC reference rows plus the Superadmin only if its email is absent.
- [ ] Test login, invalid password, refresh rotation, logout revocation, forbidden permission, and a user trying to read an unassigned warehouse.

**Verification:** Unauthenticated calls return 401, unauthorized calls return 403, and revoked tokens cannot call APIs.

### Task 5: Deliver master data APIs

**Description:** Create the reference data required by every stock transaction.

**Skill:** `backend-development`

**Files:**
- Create: `category/{entity,repository,service,controller,dto}/*`, `product/{entity,repository,service,controller,dto}/*`, `warehouse/{entity,repository,service,controller,dto}/*`, `location/{entity,repository,service,controller,dto}/*`
- Test: `category/service/ProductCategoryServiceTest.java`, `product/service/ProductServiceTest.java`, `location/service/WarehouseLocationServiceTest.java`

- [ ] Implement CRUD endpoints for product categories, products, warehouses, and locations under `/api/v1`.
- [ ] Validate unique category code, SKU, warehouse code, and per-warehouse location code; validate `minimumStock >= 0` and active parent records.
- [ ] Support paginated list endpoints with explicit filter and sort allow-lists. Fetch only the relationships each response needs to prevent N+1 queries.
- [ ] Soft-delete only unused references; reject deletion with 409 if a transactional record refers to the row.
- [ ] Cache category, product, warehouse, and location read lists in Redis, then evict their keys on mutations.
- [ ] Test CRUD, duplicate conflicts, soft deletion, warehouse-scoped locations, pagination, and cache eviction.

**Verification:** A user can create valid master data, but cannot duplicate codes or view an unassigned warehouse's locations.

### Task 6: Implement inbound workflow and putaway

**Description:** Add pending-to-approved receiving and the first positive stock movement.

**Skill:** `backend-development`

**Files:**
- Create: `inbound/{entity,repository,service,controller,dto}/*`, `inventory/StockBalanceService.java`, `inventory/StockMovementService.java`
- Test: `inbound/InboundIntegrationTest.java`

- [ ] Define `PENDING`, `APPROVED`, and `REJECTED` transaction status enums. Generate unique reference numbers in the service, not the controller.
- [ ] Implement inbound draft create/update/delete, submit, list/detail, approve, and reject endpoints. Only creator staff can revise a rejected request; only authorized supervisors can review.
- [ ] Implement approved-inbound putaway to a location. In one transaction, lock/create the balance row, add quantity, insert an `INBOUND`/`IN` movement with resulting balance, and write audit records.
- [ ] Block second putaway, use of a foreign location, inactive products, and non-approved inbounds.
- [ ] Test approval does not change stock, putaway changes balance exactly once, and movement/audit rows are present.

**Verification:** An approved inbound creates stock only when staff posts it to an allowed warehouse location.

### Task 7: Implement stock query, movement history, and concurrency rules

**Description:** Make the stock ledger observable and safe for concurrent approvals.

**Skill:** `backend-development`

**Files:**
- Create: `inventory/StockBalanceRepository.java`, `inventory/StockQueryService.java`, `inventory/StockMovementController.java`
- Modify: `inventory/StockBalanceService.java`
- Test: `inventory/StockConcurrencyIntegrationTest.java`, `inventory/StockQueryIntegrationTest.java`

- [ ] Add a repository method that selects the balance by location and product with `PESSIMISTIC_WRITE`.
- [ ] Reject a mutation when its calculated balance is negative before persisting either balance or movement.
- [ ] Implement paginated `GET /api/v1/inventory/stocks` and `GET /api/v1/inventory/movements`, filtered only to warehouses the user may access.
- [ ] Start two concurrent outbound-like deductions against the dedicated HomeServer test database. Assert exactly one succeeds when their combined quantity exceeds the balance.
- [ ] Test that each successful mutation creates one movement with correct direction, amount, and `stockAfter`.

**Verification:** Concurrent changes never leave a negative balance or an orphan movement.

### Task 8: Implement stock transfer approval

**Description:** Move stock between locations without changing total warehouse stock.

**Skill:** `backend-development`

**Files:**
- Create: `transfer/*`
- Test: `transfer/StockTransferIntegrationTest.java`

- [ ] Implement draft/revise/submit/list/detail/approve/reject endpoints for stock transfers and item validation.
- [ ] On approval, lock source and destination balance rows in stable UUID order; subtract source, add destination, write an `OUT` and an `IN` `STOCK_TRANSFER` movement for each item, and commit once.
- [ ] Reject equal source/destination locations, locations outside the stated warehouse, insufficient source stock, duplicate product items, and a second approval.
- [ ] Test total quantity remains constant, locations change by the requested amount, and an approval rollback restores both balances when one item fails.

**Verification:** Transfer preserves total stock and yields two traceable movements per item.

### Task 9: Implement stock adjustment approval

**Description:** Record justified positive or negative corrections.

**Skill:** `backend-development`

**Files:**
- Create: `adjustment/*`
- Test: `adjustment/StockAdjustmentIntegrationTest.java`

- [ ] Implement adjustment types `STOCK_OPNAME`, `DAMAGED_GOODS`, `LOST_GOODS`, and `CORRECTION`, plus a mandatory reason and signed item quantity change.
- [ ] Implement the standard draft/revise/submit/approve/reject lifecycle.
- [ ] On approval, lock each balance, apply signed change, reject a negative result, and write one `STOCK_ADJUSTMENT` movement using `IN` or `OUT` direction.
- [ ] Test reasons are required, negative stock is rejected atomically, positive adjustment increases balance, and audit records capture reviewer and reason.

**Verification:** Adjustments never bypass approval or the immutable movement ledger.

### Task 10: Implement outbound approval

**Description:** Remove stock only after supervisor approval.

**Skill:** `backend-development`

**Files:**
- Create: `outbound/*`
- Test: `outbound/OutboundIntegrationTest.java`

- [ ] Implement draft/revise/submit/list/detail/approve/reject endpoints and validate each product/location item belongs to the selected warehouse.
- [ ] In approval, lock all affected balances in stable order, validate availability, decrement them, write `OUTBOUND`/`OUT` movements, update reviewer timestamps, and create audit rows in one transaction.
- [ ] Test rejected requests may be revised, approved requests cannot be changed, insufficient stock yields 422 with no partial update, and warehouse access is enforced.

**Verification:** Outbound approval is atomic and cannot make stock negative.

### Task 11: Build dashboard, reports, and audit queries

**Description:** Expose read models without loading all transactions into memory.

**Skill:** `backend-development`

**Files:**
- Create: `reporting/DashboardService.java`, `reporting/StockReportService.java`, `audit/AuditTrailController.java`
- Test: `reporting/ReportingIntegrationTest.java`, `audit/AuditTrailIntegrationTest.java`

- [ ] Implement `GET /api/v1/dashboard?warehouseId=` with total product, total stock, low/out-of-stock product, and each pending transaction count. Verify warehouse access first.
- [ ] Implement stock summary and stock-movement reports with bounded pagination, date range filters, and index-aligned projections.
- [ ] Implement read-only paginated audit-trail endpoint with entity, action, user, and date filters.
- [ ] Cache dashboard and stock-summary queries with a defined TTL; evict their warehouse keys after all stock and master-data mutations.
- [ ] Test counts, filters, access denial, and cache invalidation after an approved transaction.

**Verification:** Reports reflect transactional data for only the caller's warehouses.

### Task 12: Implement asynchronous exports and Mailpit notifications

**Description:** Generate report files outside the request thread through Redis Streams.

**Skill:** `backend-development`

**Files:**
- Create: `reporting/export/{ExportJob,ExportPublisher,ExportConsumer,ExportStorageService,ExportMailService}.*`, `config/RedisStreamConfig.java`, `config/MailConfig.java`
- Create: `db/migration/V9__create_export_jobs.sql`
- Test: `reporting/export/ExportJobIntegrationTest.java`

- [ ] Add an export-job table with requester, warehouse, report type, filters JSON, status, output path, error message, and timestamps.
- [ ] Implement `POST /api/v1/reports/exports` returning 202 and a job ID, `GET /api/v1/reports/exports/{id}`, and an authorized download endpoint for completed jobs.
- [ ] Publish the job ID to a named Redis Stream. Create a consumer group, claim/retry pending messages with a bounded retry count, acknowledge only after DB status and email result are recorded.
- [ ] Generate CSV with a streaming writer, store it below configurable `WMS_EXPORT_DIRECTORY`, send its Mailpit link or attachment, and never expose another user's export.
- [ ] Unit-test queued, running, completed, failed, retry, and access-denied states with a fake stream gateway and fake mail sender. Manually send one `dev` email and inspect it in Mailpit.

**Verification:** The HTTP request returns quickly, the worker finishes independently, and failed jobs retain a safe error message.

### Task 13: Publish the API contract and learning guide

**Description:** Make the finished service discoverable and repeatable for the learner.

**Skill:** `design-openapi-contract`

**Files:**
- Create: `docs/openapi/openapi.yaml`, `docs/postman/warehouse-management.postman_collection.json`, `README.md`
- Modify: `build.gradle`

- [ ] Document every endpoint from Tasks 4–12, bearer security, envelope schemas, request validation, workflow errors, pagination, and examples.
- [ ] Add springdoc annotations matching the published contract and configure Swagger UI at `/swagger-ui/index.html`.
- [ ] Create a Postman collection ordered as: login, master data, inbound/putaway, transfer, adjustment, outbound, reporting/export. Store base URL and tokens as collection variables.
- [ ] Write README setup steps: copy `.env.example`, enter HomeServer credentials, choose the Spring profile, migrate, run tests, open Swagger, and inspect Mailpit.
- [ ] Run `./gradlew test`, start the app with the dev environment, and confirm Swagger lists all controller paths.

**Verification:** A new learner can configure existing containers, authenticate, execute the main flow, and inspect its email without Docker Compose.

### Task 14: Execute final quality gates

**Description:** Prove the complete build, migrations, and critical workflow before handoff.

**Skill:** `verification-before-completion`

**Files:**
- Modify: only defects discovered by verification

- [ ] Run `./gradlew clean test` for unit tests, then run the opt-in HomeServer integration suite with its dedicated test credentials.
- [ ] Run the application with a blank development database and confirm Flyway reaches the latest version.
- [ ] Execute the Postman critical flow: login → master data → inbound → putaway → transfer → adjustment → outbound → stock movement → export.
- [ ] Verify no local `.env` file, password, JWT, export data, or HomeServer credential appears in `git status`.
- [ ] Commit each independently testable task with conventional messages, then create a final `docs: add warehouse-management API guide` commit.

**Verification:** The build is green, the critical flow preserves stock invariants, and no secret is tracked.

## Self-review

| Requirement | Plan coverage |
| --- | --- |
| Authentication, RBAC, warehouse visibility | Task 4 and integration tests |
| Master data | Task 5 |
| Inbound, outbound, transfer, adjustment lifecycle | Tasks 6, 8, 9, and 10 |
| Non-negative, consistent stock and movement history | Tasks 6–10 |
| Concurrency and atomic rollback | Tasks 7, 8, and 10 |
| Dashboard, reports, export, email | Tasks 11–12 |
| Audit trail, OpenAPI, Postman, setup | Tasks 3, 11, and 13 |

No scope item from the approved design is unassigned. Property names remain consistent: `warehouseLocationId`, `productId`, `quantityChange`, `referenceNumber`, and `stockAfter` are the API/Java names; migrations map them to snake_case columns.
