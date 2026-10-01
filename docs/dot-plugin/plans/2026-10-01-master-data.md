# Master Data Implementation Plan

**Goal:** Implement secured CRUD APIs for categories, products, warehouses, and warehouse locations.

## Tasks

### Task 1: Category and product

- [x] Create entity, repository, service, request/response DTOs, and controller under `category` and `product`.
- [x] Enforce unique code/SKU, active category, and non-negative minimum stock.
- [x] Apply `@PreAuthorize` for PRODUCTS and PRODUCT_CATEGORIES permissions.
- [x] Add paginated lists with allow-listed sort fields.

### Task 2: Warehouse and location

- [x] Create equivalent layers under `warehouse` and `location`.
- [x] Enforce unique warehouse code and `(warehouseId, locationCode)`.
- [x] Apply permissions and `WarehouseAccessService` to every scoped read and mutation.

### Task 3: Soft delete, cache, and tests

- [x] Reject deletion of rows with live dependent records using 409.
- [x] Cache category and product lists, and evict their entries on mutations. Warehouse and location lists are queried per request to reflect current user assignments.
- [x] Add unit tests and run `./gradlew clean test`.

### Task 4: Align package structure with auth

- [x] Move `category`, `product`, `warehouse`, and `location` directly under `com.warehousing.wmsapi`; remove the intermediate package.
- [x] Within each feature, place persistence models in `entity`, database interfaces in `repository`, business logic in `service`, controllers in `controller`, and request/response objects in `dto`.
- [x] Move service tests to matching feature `service` packages and update imports without changing routes, database mappings, or behavior.
- [x] Confirm there are no old package references and run `./gradlew clean test`.

### Task 5: Unify controller and DTO package names

- [x] Move the four feature controllers to their `controller` packages and all request/response objects to their feature `dto` packages.
- [x] Apply the same layout to `auth` so the implemented modules use one package convention.
- [x] Update imports and run `./gradlew clean test` without changing endpoint paths or payloads.
