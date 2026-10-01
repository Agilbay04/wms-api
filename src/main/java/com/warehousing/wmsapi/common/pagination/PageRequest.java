package com.warehousing.wmsapi.common.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageRequest(
        @Min(value = 1, message = "page must be at least one") int page,
        @Min(value = 1, message = "size must be at least one")
        @Max(value = 100, message = "size must not exceed 100") int size
) {
}
