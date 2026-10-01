# Dev Master Seeder and One-Based Pagination Implementation Plan

> **For agentic workers:** Use `dot-plugin:executing-plans` to implement this approved plan task by task. Run each verification before marking a task complete.

**Goal:** Provide safe development master data and make list pagination start at page 1 with default size 10.
**Architecture:** A dev-only `ApplicationRunner` reads four small CSV fixtures and inserts missing rows through `JdbcTemplate` in dependency order. `SEEDER_ENABLE_DB_SETUP` gates all new master-data writes. The result is written after database commit into a dedicated directory, backed by a Docker named volume. Pagination converts public one-based page numbers to zero-based JPA indexes and SQL offsets.
**Tech stack:** Java 17, Spring Boot, Spring Data JPA, PostgreSQL, JUnit 5, Mockito.

## Tasks

### Task 1: One-based master-data pagination

**Description:** Keep the HTTP contract one-based while preserving JPA's zero-based indexing.
**Skill:** `dot-plugin:backend-development`

**Files:** `src/main/java/com/warehousing/wmsapi/common/pagination/MasterPage.java`, `src/main/java/com/warehousing/wmsapi/common/pagination/PageRequest.java`, the four master-data controllers, `src/main/java/com/warehousing/wmsapi/warehouse/service/WarehouseServiceImpl.java`, `src/test/java/com/warehousing/wmsapi/common/pagination/MasterPageTest.java`, `src/test/java/com/warehousing/wmsapi/common/pagination/MasterListDefaultsTest.java`, `src/test/java/com/warehousing/wmsapi/warehouse/service/WarehousePaginationTest.java`, `docs/dot-plugin/specs/2026-10-01-master-data-design.md`.

- [x] Change all four list controller parameters to `@RequestParam(defaultValue = "1") int page` and `@RequestParam(defaultValue = "10") int size`.
- [x] Reject `page < 1`; call `PageRequest.of(page - 1, size, Sort.by(sort).ascending())` from `MasterPage.of`. Set record validation to `@Min(value = 1, message = "page must be at least one")`.
- [x] In warehouse listing, use the same one-based validation and calculate `OFFSET` as `(long) (page - 1) * size`. Return the public page unchanged in `PageResponse`.
- [x] Update `MasterPageTest`: page 1 maps to JPA page 0; page 2 maps to JPA page 1; page 0 and invalid size/sort throw `INVALID_PAGE_REQUEST`. Test controller defaults through `@RequestParam` reflection.
- [x] Run `./gradlew test --offline --tests '*MasterPageTest'`; expect `BUILD SUCCESSFUL`.

### Task 2: Safe development master-data seeder

**Description:** Insert sample master data once per natural key, preserve manually edited rows, and record newly created IDs.
**Skill:** `dot-plugin:api-automation-test-seeder` and `dot-plugin:backend-development`

**Files:** `config/DevMasterDataSeeder.java`, `config/SeederProperties.java`, `src/main/resources/seed/categories.csv`, `products.csv`, `warehouses.csv`, `locations.csv`, `application.properties`, `.env.example`, `.gitignore`, `Dockerfile`, `docker-compose.yml`, `config/DevMasterDataSeederTest.java`, `docs/docker.md`.

- [x] Add `seeder.enable-db-setup=${SEEDER_ENABLE_DB_SETUP:false}` with typed configuration metadata and `SEEDER_ENABLE_DB_SETUP=false` in `.env.example`; leave `.env` unchanged.
- [x] Provide CSV payloads with three categories, twelve products, two warehouses, and four locations. Products reference category code; locations reference warehouse code.
- [x] Implement `@Component @Profile("dev") @ConditionalOnProperty(name = "seeder.enable-db-setup", havingValue = "true")` on `DevMasterDataSeeder`. Its `run` reads the CSVs, inserts categories and warehouses before their children inside a `TransactionTemplate`, and uses `INSERT ... ON CONFLICT DO NOTHING RETURNING id` to avoid overwriting existing rows. Resolve existing active parent IDs by natural key; fail if a parent is inactive or soft-deleted.
- [x] Record only newly inserted IDs in `seed-results/seed-runs/<timestamp>/seed-result.json` after commit. Include resource key, table, ID, `created-db` action, and ID-scoped manual cleanup metadata; never include credentials. Do not implement or run automatic cleanup.
- [x] Unit-test insertion order, existing-row skip, disabled configuration annotation, and result metadata with mocked `JdbcTemplate` or isolated filesystem. The test must not reach HomeServer.
- [x] Document the opt-in flag, sample rows, result location, and manual verification commands. State that database setup is not executed by automated tests.
- [x] Run `./gradlew test --offline` and `./gradlew build --offline`; expect both to finish with `BUILD SUCCESSFUL`.

## Safety and completion

Do not start the application or set `SEEDER_ENABLE_DB_SETUP=true` against HomeServer during implementation. Review the final files and report the exact test results. No commit is required because the repository currently has no commits and all existing project files are untracked.
