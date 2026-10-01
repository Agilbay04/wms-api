package com.warehousing.wmsapi.common.api;

import java.io.Serializable;
import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) implements Serializable {
}
