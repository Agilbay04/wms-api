# Auth Package Structure Refactor Plan

> **For agentic workers:** Use `dot-plugin:executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Issue:** Separate HTTP, service, and security responsibilities inside the `auth` feature.

**Goal:** Replace the flat `auth` package with focused subpackages without changing runtime behavior.

**Architecture:** `auth.controller` contains the HTTP controller, `auth.dto` contains request/response objects, `auth.service` contains use-case services, and `auth.security` contains JWT and Spring Security integration. `SecurityConfig` imports moved security classes; moved classes import one another across their explicit package boundaries.

**Tech stack:** Java 17, Spring Boot 4.1.1, Spring Security, JUnit 5.

## File structure

| Area | Responsibility |
| --- | --- |
| `auth/controller` | HTTP controller. |
| `auth/dto` | Request and response DTOs. |
| `auth/service` | Login and refresh-token application services. |
| `auth/security` | JWT creation, token filter, and database-backed user details. |
| `auth/security/JwtServiceTest` | Unit test for the moved JWT service. |
| `config/SecurityConfig.java` | Imports the JWT filter and user-details service from their new package. |

## Tasks

### Task 1: Move web and service classes

**Description:** Put HTTP types and application services in packages that state their responsibility.

**Skill:** `backend-development`

**Files:**

- Move: `auth/AuthController.java` → `auth/controller/AuthController.java`
- Move: `auth/LoginRequest.java` → `auth/dto/LoginRequest.java`
- Move: `auth/RefreshTokenRequest.java` → `auth/dto/RefreshTokenRequest.java`
- Move: `auth/LoginResponse.java` → `auth/dto/LoginResponse.java`
- Move: `auth/AuthService.java` → `auth/service/AuthService.java`
- Move: `auth/RefreshTokenService.java` → `auth/service/RefreshTokenService.java`

- [x] **Step 1: Move the DTOs and change their package declarations.**

```java
package com.warehousing.wmsapi.auth.dto;
```

Use the declaration above for `LoginRequest` and `RefreshTokenRequest`. Use this declaration for `LoginResponse`:

```java
package com.warehousing.wmsapi.auth.dto;
```

- [x] **Step 2: Move the two services and change their package declarations.**

```java
package com.warehousing.wmsapi.auth.service;
```

Add imports for `LoginRequest`, `RefreshTokenRequest`, and `LoginResponse` where each type is used.

- [x] **Step 3: Move the controller and change its package declaration.**

```java
package com.warehousing.wmsapi.auth.controller;
```

Import `AuthService` from `auth.service` and request and response DTOs from `auth.dto`.

- [x] **Step 4: Compile the moved classes.**

Run: `./gradlew compileJava`

Expected: `BUILD SUCCESSFUL` with no unresolved package or import error.

### Task 2: Move security classes and update consumers

**Description:** Keep JWT and Spring Security integration together and update all imports.

**Skill:** `backend-development`

**Files:**

- Move: `auth/JwtService.java` → `auth/security/JwtService.java`
- Move: `auth/JwtAuthenticationFilter.java` → `auth/security/JwtAuthenticationFilter.java`
- Move: `auth/DatabaseUserDetailsService.java` → `auth/security/DatabaseUserDetailsService.java`
- Modify: `auth/service/AuthService.java`
- Modify: `config/SecurityConfig.java`

- [x] **Step 1: Move all three security classes and set their package declaration.**

```java
package com.warehousing.wmsapi.auth.security;
```

- [x] **Step 2: Update imports in `AuthService`.**

```java
import com.warehousing.wmsapi.auth.security.JwtService;
import com.warehousing.wmsapi.auth.dto.LoginRequest;
import com.warehousing.wmsapi.auth.dto.RefreshTokenRequest;
import com.warehousing.wmsapi.auth.dto.LoginResponse;
```

- [x] **Step 3: Update imports in `SecurityConfig`.**

```java
import com.warehousing.wmsapi.auth.security.DatabaseUserDetailsService;
import com.warehousing.wmsapi.auth.security.JwtAuthenticationFilter;
```

- [x] **Step 4: Compile the security configuration.**

Run: `./gradlew compileJava`

Expected: `BUILD SUCCESSFUL` with component scanning still covering each moved `@Service` and `@Component`.

### Task 3: Move test and verify behavior

**Description:** Keep the JWT test next to the security package and run the complete unit suite.

**Skill:** `backend-development`

**Files:**

- Move: `src/test/java/com/warehousing/wmsapi/auth/JwtServiceTest.java` → `src/test/java/com/warehousing/wmsapi/auth/security/JwtServiceTest.java`

- [x] **Step 1: Change the test package declaration.**

```java
package com.warehousing.wmsapi.auth.security;
```

- [x] **Step 2: Run the JWT test.**

Run: `./gradlew test --tests com.warehousing.wmsapi.auth.security.JwtServiceTest`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 3: Run the full unit suite and inspect formatting.**

Run: `./gradlew clean test && git diff --check`

Expected: `BUILD SUCCESSFUL` and no whitespace errors.

**Verification:** The source tree has no Java file directly under `auth`, all auth imports use `web`, `service`, or `security`, and the complete test suite passes.
