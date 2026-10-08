package com.warehousing.wmsapi.transfer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StockTransferItemRequest(@NotNull UUID productId, @NotNull UUID sourceWarehouseLocationId,
        @NotNull UUID destinationWarehouseLocationId, @Min(1) int quantity, @Size(max = 1000) String notes) {
}
