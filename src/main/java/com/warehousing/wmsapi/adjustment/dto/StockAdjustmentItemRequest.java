package com.warehousing.wmsapi.adjustment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StockAdjustmentItemRequest(@NotNull UUID productId, @NotNull UUID warehouseLocationId,
        @NotNull Integer quantityChange, @Size(max = 1000) String notes) {
}
