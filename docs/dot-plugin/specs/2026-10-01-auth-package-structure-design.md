# Auth Package Structure Design

## Chosen approach

The `auth` feature remains one bounded context, but its classes move into packages that reflect their responsibility:

```text
auth/
  web/
    AuthController.java
    request/
      LoginRequest.java
      RefreshTokenRequest.java
    response/
      LoginResponse.java
  service/
    AuthService.java
    RefreshTokenService.java
  security/
    JwtService.java
    JwtAuthenticationFilter.java
    DatabaseUserDetailsService.java
```

This keeps feature ownership clear while reducing the number of unrelated classes in the root `auth` package. `web` owns HTTP request and response types, `service` owns login and refresh-token use cases, and `security` owns Spring Security and JWT integration.

The refactor changes Java package declarations and imports only. Spring component scanning continues from `com.warehousing.wmsapi`, so all moved components remain discovered automatically.

## Database design

No database schema, migration, Redis key, or seed-data change is required.

## OpenAPI design

No endpoint, request field, response field, authentication rule, or OpenAPI contract changes. Auth endpoints remain under `/api/v1/auth`.

## Testing strategy

Move `JwtServiceTest` to the matching `auth/security` test package and update imports. Run `./gradlew clean test` to prove that package declarations, component discovery, and existing unit tests still compile and pass.
