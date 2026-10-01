package com.warehousing.wmsapi.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProductRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotBlank @Size(max = 50) String unit,
        @Min(0) int minimumStock,
        boolean active
) {
}
