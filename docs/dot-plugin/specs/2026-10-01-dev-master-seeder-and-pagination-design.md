# Dev Master Seeder and One-Based Pagination Design

## Chosen approach

Extend the existing development-only startup seeding pattern with a separate master-data seeder. It inserts sample categories, products, warehouses, and warehouse locations in dependency order. The seeder runs only with the `dev` profile and `SEEDER_ENABLE_DB_SETUP=true`; its default is disabled. Natural keys (category code, product SKU, warehouse code, and warehouse plus location code) make repeated runs idempotent. Existing rows are neither updated nor deleted. After the database transaction commits, the seeder records only newly created row IDs in a `seed-result.json` artifact, with no credentials or tokens. The result directory is independent from exports and uses a Docker named volume. Cleanup remains manual and scoped to those IDs; no automatic delete is introduced.

Use three categories, twelve products, two warehouses, and four locations. Twelve products make the second page visible with the new default size of ten. Keep sample payloads separate from seeding logic so they are easy to inspect while learning.

## Database design

No schema or Flyway migration changes. The existing `product_categories`, `products`, `warehouses`, and `warehouse_locations` tables remain the source of truth. Seeder insertion follows their foreign-key order and uses a transaction. Existing RBAC and Superadmin seeding remains unchanged.

## OpenAPI design

No new routes or response fields. The list endpoints for `/api/v1/product-categories`, `/api/v1/products`, `/api/v1/warehouses`, and `/api/v1/warehouses/{warehouseId}/locations` default to `page=1&size=10`. Public page numbers start at one. The service converts `page` to the zero-based JPA or SQL offset and returns the original public page number in `PageResponse.page`. `page=0`, `size=0`, `size>100`, and unsupported sort fields return HTTP 400. Explicit valid `page` and `size` remain supported.

## Testing strategy

Use the project's JUnit 5 and Mockito tests. Unit tests cover one-based page conversion, invalid page rejection, controller defaults, SQL offset for warehouse lists, and seeder gating/idempotence/dependency order. No HomeServer database mutation is part of automated verification. Run the full Gradle test suite and build; manual API verification is optional after the user enables the seeder and starts the `dev` profile.
