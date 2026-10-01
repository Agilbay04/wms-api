
package com.warehousing.wmsapi.common.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.warehousing.wmsapi.common.api.ApiResponse;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler exceptionHandler = new ApiExceptionHandler();

    @Test
    void shouldMapBusinessExceptionToItsDeclaredHttpStatusCode() {
        BusinessException exception = new BusinessException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "INSUFFICIENT_STOCK",
                "Stock is insufficient."
        );

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(exception);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals(422, response.getBody().code());
        assertEquals("Stock is insufficient.", response.getBody().message());
        assertEquals(null, response.getBody().data());
        assertEquals(Map.of(), response.getBody().errors());
    }

    @Test
    void shouldHideUnexpectedExceptionDetails() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUnexpectedException(
                new IllegalStateException("database hostname should not be exposed")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals(500, response.getBody().code());
        assertEquals("An unexpected error occurred.", response.getBody().message());
    }

    @Test
    void shouldMapMethodAuthorizationFailureToForbidden() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAccessDenied(
                new AccessDeniedException("internal permission detail"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals(403, response.getBody().code());
    }

    @Test
    void shouldMapMissingResourceToNotFoundCode() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNotFound(new NoSuchElementException());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().code());
    }
}
