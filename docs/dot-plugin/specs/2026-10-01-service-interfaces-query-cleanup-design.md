# Service Interfaces and Query Cleanup Design

## Chosen approach

Expose application services through interfaces and keep their Spring implementations in classes named `*Impl`. This applies to auth, refresh tokens, categories, products, warehouses, locations, permission checks, and warehouse-access checks. Controllers and other services depend on the interfaces. Keep the existing Spring Data repository interfaces; Spring supplies their implementations.

Replace the development seeder's simple user-existence SQL with `UserRepository.existsByEmail`. Keep SQL that joins RBAC tables, performs idempotent bulk seeding, filters warehouses by current assignment, or checks references in transaction tables that do not yet have JPA entities. Do not add entity mappings only to remove these queries.

## Database design

No schema or migration changes. Existing table constraints and seed behavior remain unchanged.

## OpenAPI design

No endpoint, authorization rule, status code, or payload changes. Service interfaces only alter Java dependencies.

## Testing strategy

Use the existing JUnit 5 and Mockito tests. Instantiate `*Impl` in unit tests and mock interfaces for dependencies. Verify seeder existence behavior with the derived repository method. Run `./gradlew clean test --offline` and `./gradlew build --offline`; no HomeServer integration database is required.
