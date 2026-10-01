package com.warehousing.wmsapi.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiResponseTest {

    @Test
    void shouldUseHttpStatusCodeForSuccess() {
        ApiResponse<String> response = ApiResponse.success(HttpStatus.CREATED, "Created.", "item");

        assertTrue(response.success());
        assertEquals(201, response.code());
        assertEquals("item", response.data());
        assertEquals(Map.of(), response.errors());
    }

    @Test
    void shouldUseHttpStatusCodeForFailure() {
        ApiResponse<Void> response = ApiResponse.failure(HttpStatus.NOT_FOUND, "Not found.", Map.of());

        assertFalse(response.success());
        assertEquals(404, response.code());
        assertEquals(null, response.data());
    }
}
