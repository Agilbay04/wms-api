# Master Data API Design

## Chosen approach

Master data is implemented as four top-level feature packages under `com.warehousing.wmsapi`: `category`, `product`, `warehouse`, and `location`. There is no intermediate `master` package. Each feature separates persistence models into `entity`, database access into `repository`, business logic into `service`, HTTP controllers into `controller`, and request/response objects into a shared `dto` package. Entities do not depend on HTTP DTOs. All responses use `ApiResponse`; lists place `PageResponse` in `data`.

Category and product list responses use Redis cache and evict their entries after mutations. Warehouse and location lists are queried on each request because their visibility depends on current user assignments.

Superadmin uses CREATE, READ, UPDATE, and DELETE permissions. Staff and Supervisor use READ permissions only. Warehouse and location reads additionally require `WarehouseAccessService`; Superadmin bypasses warehouse assignment.

Product category codes, SKUs, warehouse codes, and per-warehouse location codes are unique. Products require an active category. `minimumStock` is non-negative. Deletes are soft deletes and reject with 409 when a remaining live relation uses the row.

## Database design

No migration is required. The existing V2 master tables and constraints are the source of truth. Entities extend `AuditableEntity` and map `is_active` explicitly.

## OpenAPI design

CRUD routes live under `/api/v1`. Location routes nest beneath their warehouse. List routes accept `page`, `size`, and a constrained `sort` value. Mutations require a matching `@PreAuthorize` permission; warehouse-scoped routes also verify the passed warehouse ID.

Supported list sort values are `code`, `name`, and `createdAt` for categories, warehouses, and locations; products use `sku`, `name`, and `createdAt`. Public page numbers start at one, default to one, and use a default size of ten. Sizes range from 1 to 100.

## Testing strategy

JUnit unit tests cover pagination validation, inactive parent references, conflicts, and warehouse scope. Manual read-only HTTP smoke tests cover authenticated lists, Redis cache reads, invalid pagination, and unauthenticated access. Automated database integration tests remain opt-in until dedicated HomeServer test credentials are available.
