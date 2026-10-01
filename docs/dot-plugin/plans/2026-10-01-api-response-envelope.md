# API Response Envelope Implementation Plan

> **For agentic workers:** Use `dot-plugin:executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Issue:** Standardize WMS API success and error bodies after reviewing `tax-system-service`.

**Goal:** Keep a single safe API envelope with `success` and an integer `code` matching the HTTP status.

**Architecture:** `ApiResponse<T>` remains the only top-level response type. Controllers use its success factory, and `ApiExceptionHandler` uses its failure factory. `PageResponse<T>` remains nested in `data` for paginated endpoints.

**Tech stack:** Java 17, Spring Boot 4.1.1, Spring MVC, JUnit 5.

## File structure

| File | Responsibility |
| --- | --- |
| `src/main/java/com/warehousing/wmsapi/common/api/ApiResponse.java` | Defines the common response body and factory methods. |
| `src/main/java/com/warehousing/wmsapi/common/error/ApiExceptionHandler.java` | Maps exceptions to HTTP status and safe failed response bodies. |
| `src/test/java/com/warehousing/wmsapi/common/error/ApiExceptionHandlerTest.java` | Guards success flag and internal-detail hiding behavior. |
| `docs/dot-plugin/specs/2026-10-01-api-response-envelope-design.md` | Records the contract decision. |

## Tasks

### Task 1: Standardize the response envelope

**Description:** Keep `success` in the generic response body and use the numeric HTTP status as `code` for success and error responses.

**Skill:** `backend-development`

**Files:**

- Modify: `src/main/java/com/warehousing/wmsapi/common/api/ApiResponse.java`
- Modify: `src/main/java/com/warehousing/wmsapi/common/error/ApiExceptionHandler.java`
- Modify: `src/test/java/com/warehousing/wmsapi/common/error/ApiExceptionHandlerTest.java`

- [ ] **Step 1: Extend the existing test with the expected envelope fields.**

```java
assertEquals(false, response.getBody().success());
assertEquals(422, response.getBody().code());
assertEquals("Stock is insufficient.", response.getBody().message());
assertEquals(null, response.getBody().data());
assertEquals(Map.of(), response.getBody().errors());
```

For the unexpected-error test, assert `success()` is `false` and continue asserting that the internal exception message is absent from `message()`.

- [ ] **Step 2: Run the focused test before changing production code.**

Run: `./gradlew test --tests com.warehousing.wmsapi.common.error.ApiExceptionHandlerTest`

Expected: compilation fails because `ApiResponse` has no `success()` accessor.

- [ ] **Step 3: Change `ApiResponse` to include the boolean and set it only in factory methods.**

```java
public record ApiResponse<T>(
        boolean success,
        int code,
        String message,
        T data,
        Map<String, String> errors
) {
    public static <T> ApiResponse<T> success(HttpStatusCode status, String message, T data) {
        return new ApiResponse<>(true, status.value(), message, data, Map.of());
    }

    public static ApiResponse<Void> failure(HttpStatusCode status, String message, Map<String, String> errors) {
        return new ApiResponse<>(false, status.value(), message, null, errors);
    }
}
```

- [ ] **Step 4: Keep `ApiExceptionHandler` as the single error mapping point.**

Do not add a second status field, a second error DTO, or a stack-trace field. Preserve the existing HTTP mapping: 400 malformed body, 404 absent resource, 409 data conflict, 422 validation, and 500 unexpected error. The handler must pass the HTTP status to `ApiResponse.failure(...)`.

- [ ] **Step 5: Run focused tests after implementation.**

Run: `./gradlew test --tests com.warehousing.wmsapi.common.error.ApiExceptionHandlerTest`

Expected: `BUILD SUCCESSFUL` and both tests pass.

- [ ] **Step 6: Run the full unit suite.**

Run: `./gradlew clean test`

Expected: `BUILD SUCCESSFUL` with all project tests passing.

- [ ] **Step 7: Review the changed contract.**

Verify these examples manually:

```json
{"success":true,"code":200,"message":"Login successful.","data":{},"errors":{}}
```

```json
{"success":false,"code":422,"message":"Request validation failed.","data":null,"errors":{"email":"must be a well-formed email address"}}
```

**Verification:** Every controller response and every `@RestControllerAdvice` response serializes with `success`, `code`, `message`, `data`, and `errors`; no response includes a stack trace, an exception message, or a duplicate HTTP status field.
