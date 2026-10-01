# Service Interfaces and Query Cleanup Plan

**Goal:** Use service interfaces with `*Impl` implementations and replace avoidable raw SQL.

**Design:** `docs/dot-plugin/specs/2026-10-01-service-interfaces-query-cleanup-design.md`

## Task 1: Replace avoidable existence SQL

- [x] Add `boolean existsByEmail(String email)` to `src/main/java/com/warehousing/wmsapi/iam/repository/UserRepository.java`.
- [x] Inject `UserRepository` into `src/main/java/com/warehousing/wmsapi/config/DevDataSeeder.java` and use `existsByEmail(admin.email())` before inserting the Superadmin.
- [x] Keep parameterized SQL for RBAC joins, idempotent bulk seed inserts, visibility filtering, and transaction-reference checks because those tables are not mapped as JPA entities.

## Task 2: Define service contracts and implementations

- [x] In `auth/service`, split `AuthService` and `RefreshTokenService` into same-name interfaces and `AuthServiceImpl` and `RefreshTokenServiceImpl` classes.
- [x] In each of `category`, `product`, `warehouse`, and `location`, split the current service into a same-name interface and a `*Impl` class. Keep cache and transaction annotations on implementation methods.
- [x] Split `common/security/PermissionService` and `WarehouseAccessService` into interfaces and `*Impl` classes. Preserve Spring bean names `permissionService` and `warehouseAccessService` for method-security expressions.
- [x] Keep `JwtService` and `DatabaseUserDetailsService` unchanged: they are security integration components, not application-service contracts.

## Task 3: Update consumers and verify

- [x] Inject service interfaces in controllers, services, and security components.
- [x] Update unit tests to instantiate implementation classes while mocking interface dependencies.
- [x] Run `./gradlew clean test --offline` and `./gradlew build --offline`; verify no unmapped raw SQL was replaced with unsafe or behavior-changing queries.
