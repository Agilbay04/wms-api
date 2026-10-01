# Task 4 Security Completion Design

## Chosen approach

Task 4 completes the existing authentication foundation without adding a temporary business endpoint. Spring Security returns the same `ApiResponse` envelope as application errors: unauthenticated access returns HTTP 401 with numeric `code` 401; an authenticated caller without permission returns HTTP 403 with numeric `code` 403.

The dev seeder creates the complete RBAC reference set. SUPERADMIN receives every permission. STAFF receives CREATE, READ, and UPDATE on INBOUNDS, OUTBOUNDS, STOCK_TRANSFERS, and STOCK_ADJUSTMENTS, plus READ on PRODUCTS, PRODUCT_CATEGORIES, WAREHOUSES, and WAREHOUSE_LOCATIONS. SUPERVISOR receives READ, APPROVE, and REJECT on those transaction resources, READ on the same master data, and READ and EXPORT on REPORTS. The seed Superadmin user is inserted only when its email is absent; later starts preserve its existing password.

Authorization queries ignore soft-deleted user-role, role, role-permission, permission, resource, operation, and warehouse-assignment rows. Future Task 5 controllers use these services through `@PreAuthorize`; no placeholder endpoint is added only to demonstrate authorization.

## Database design

No schema or migration change is required. This task inserts and links existing RBAC reference data only in the development profile.

## OpenAPI design

No endpoint is added. Protected endpoints now consistently return these bodies when Spring Security rejects a request:

```json
{"success":false,"code":401,"message":"Authentication is required.","data":null,"errors":{}}
```

```json
{"success":false,"code":403,"message":"You do not have permission to perform this action.","data":null,"errors":{}}
```

## Testing strategy

JUnit 5 and Mockito test login failure, refresh-token rotation and revocation, authorization query outcomes, and the two security error writers. These tests have no database, Redis, or HomeServer dependency. Full HTTP integration remains opt-in until dedicated HomeServer test credentials are supplied.
