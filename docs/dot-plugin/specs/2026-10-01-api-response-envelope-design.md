# API Response Envelope Design

## Chosen approach

WMS uses one generic API envelope for successful and failed requests. The `code` field is an integer equal to the HTTP response status (for example, 200, 201, or 404). A successful response carries `data`; a failed response carries `errors`. `success` indicates which case applies.

The numeric code makes the body status easy to read while keeping it consistent with the HTTP status line. Business exception identifiers remain internal and are not exposed as `ApiResponse.code`. Responses never include stack traces or persistence details.

```json
{
  "success": false,
  "code": 422,
  "message": "Request validation failed.",
  "data": null,
  "errors": {
    "email": "must be a well-formed email address"
  }
}
```

Successful responses use the same keys. `errors` is an empty object.

```json
{
  "success": true,
  "code": 200,
  "message": "Login successful.",
  "data": {
    "accessToken": "..."
  },
  "errors": {}
}
```

`PageResponse` remains the value of `data` for list endpoints. It keeps `content`, `page`, `size`, `totalElements`, and `totalPages`; no second list-specific envelope is introduced.

## Database design

No database schema or migration changes are required.

## OpenAPI design

No endpoint paths change. All existing and future `/api/v1` responses use the following schema:

| Field | Type | Meaning |
| --- | --- | --- |
| `success` | boolean | Whether the request completed successfully. |
| `code` | integer | HTTP status code matching the response status (for example, 201 or 404). |
| `message` | string | Human-readable summary that does not contain internal details. |
| `data` | object or null | Successful result; `PageResponse` for paginated lists. |
| `errors` | object | Field-to-message map for validation errors; empty for other responses. |

The `ApiExceptionHandler` continues to map malformed bodies to 400, authentication failures to 401, authorization failures to 403, absent resources to 404, data conflicts to 409, semantic validation failures to 422, and unexpected failures to 500. It must never expose stack traces, database messages, secrets, or internal paths.

## Testing strategy

JUnit 5 tests the response factories, `ApiExceptionHandler`, and security error writers. The tests assert `success`, numeric `code`, `message`, `data`, and `errors` for created responses, business errors, and unexpected errors. `./gradlew test` verifies compilation and the existing unit suite.
