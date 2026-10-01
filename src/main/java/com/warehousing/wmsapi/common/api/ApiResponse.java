package com.warehousing.wmsapi.common.api;

import java.util.Map;
import org.springframework.http.HttpStatusCode;

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
