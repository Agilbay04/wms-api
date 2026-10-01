# Warehouse Management Design

## Chosen approach

The application will be a modular monolith. Each bounded context owns its controller, request and response DTOs, service, repository, entity, and tests. Shared HTTP, security, persistence, and error-handling code lives in `common`. This keeps Spring's layered architecture visible without grouping unrelated business code in global controller, service, and repository folders.

The delivery covers the complete warehouse-management scope, but it builds modules in dependency order: foundation, IAM, master data, inbound, stock ledger, transfer and adjustment, outbound, reporting, and asynchronous export. The application uses stateless JWT access tokens, Redis-backed refresh-token revocation, PostgreSQL for transactional state, and Redis Streams for export jobs.

## Database design

`docs/erd/wms.dbml` is the domain reference. Flyway creates its tables and constraints. A location stock row in `warehouse_location_items` is the authoritative balance for one `(warehouse_location_id, product_id)` pair. `stock_movements` is immutable history. An approval that changes stock runs in one short transaction, locks each affected balance row with `PESSIMISTIC_WRITE`, rejects a negative resulting quantity, changes the balance, and writes its movement records.

The implementation verifies that every location belongs to the transaction warehouse. Transfer source and destination must differ. Product, warehouse, and location rows use soft deletion. A `@Version` field protects master-data edits; stock mutations use database locking. Every state-changing action writes `audit_trails` in the same transaction.

The `dev` profile seeds operations, resources, permissions, roles, role-permission links, and a single Superadmin. `WMS_SEED_ADMIN_EMAIL`, `WMS_SEED_ADMIN_NAME`, and `WMS_SEED_ADMIN_PASSWORD` supply that account; the password is BCrypt-hashed and never committed.

## OpenAPI design

All application endpoints use `/api/v1` and a common envelope: `code`, `message`, `data`, and optional `errors` or pagination metadata. Swagger UI documents the contract through springdoc annotations and secured endpoints declare bearer authentication.

- Auth: `POST /auth/login`, `POST /auth/refresh`, and `POST /auth/logout`.
- IAM: CRUD users, roles, permissions, and user-warehouse assignments.
- Master data: CRUD product categories, products, warehouses, and warehouse locations.
- Inbound: create, revise, list, detail, submit, approve, reject, and post approved goods to locations.
- Inventory: stock balances, movements, stock transfers, and stock adjustments with the same draft/submit/approve/reject lifecycle.
- Outbound: create, revise, list, detail, submit, approve, and reject.
- Reporting: stock summary, stock movement, dashboard by allowed warehouse, export request, export status, and file download.
- Audit: paginated read-only audit trails for authorized users.

List endpoints support bounded offset pagination, supported filters, and an allow-list of sort fields. Command endpoints use explicit actions because approval changes workflow state rather than replacing a resource.

## Testing strategy

JUnit 5 and Mockito test pure rules, status transitions, reference generation, and authorization helpers. Opt-in `@SpringBootTest` integration tests run against a dedicated `wms_test` PostgreSQL database and Redis instance on HomeServer, supplied only through test environment variables. They run Flyway, call real HTTP endpoints, and cover authentication, warehouse visibility, CRUD validation, approval and rejection, rollback, negative-stock prevention, movement history, cache invalidation, and token revocation.

The export worker is unit-tested with a fake mail sender. The `dev` profile is manually verified against Mailpit after the automated suite passes. Integration tests use the dedicated HomeServer test database, never the developer, staging, or production database.
