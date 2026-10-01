# Task 4 Security Completion Plan

**Goal:** Complete common authorization behavior, idempotent RBAC seed data, and unit coverage before Task 5.

## Tasks

### Task 1: Return the common envelope for Spring Security failures

**Files:** Create `common/security/ApiAuthenticationEntryPoint.java`, `common/security/ApiAccessDeniedHandler.java`; modify `config/SecurityConfig.java`; test `common/security/SecurityErrorHandlerTest.java`.

- [x] Write direct tests with `MockHttpServletResponse` asserting 401/`UNAUTHENTICATED` and 403/`FORBIDDEN`, each with `success=false`, null data, and empty errors.
- [x] Implement both handlers with `ObjectMapper` and `ApiResponse.failure`; set JSON content type and the matching HTTP status.
- [x] Register them with `http.exceptionHandling(...)` in `SecurityConfig`.
- [x] Run `./gradlew test --tests '*SecurityErrorHandlerTest'`.

### Task 2: Correct RBAC seed data and authorization queries

**Files:** Modify `config/DevDataSeeder.java`, `common/security/PermissionService.java`, and `common/security/WarehouseAccessService.java`; test `common/security/PermissionServiceTest.java` and `common/security/WarehouseAccessServiceTest.java`.

- [x] Add `WAREHOUSE_LOCATIONS` to seeded resources, create explicit STAFF and SUPERVISOR grants defined in the design, and retain the SUPERADMIN all-permission grant.
- [x] Replace the seed-user upsert with an existence check followed by INSERT only when absent; always ensure the user-role link exists.
- [x] Add `deleted_at IS NULL` checks to each RBAC and warehouse-access join.
- [x] Test unauthenticated short-circuit, JDBC true, and JDBC false outcomes with Mockito.
- [x] Run the focused authorization tests.

### Task 3: Add authentication and refresh-token unit tests

**Files:** Create `auth/service/AuthServiceTest.java` and `auth/service/RefreshTokenServiceTest.java`.

- [x] Test successful login invokes the authentication provider and returns access plus refresh tokens.
- [x] Test bad credentials and an invalid refresh token map to `BusinessException` with 401 and stable codes.
- [x] Test refresh token issue, rotate, and revoke call the expected `StringRedisTemplate` methods and use the configured TTL.
- [x] Run `./gradlew clean test && git diff --check`.

**Verification:** All unit tests pass; Spring Security uses the common error envelope; the seeder preserves an existing Superadmin password; authorization ignores soft-deleted RBAC data.
