package com.warehousing.wmsapi.outbound.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record OutboundItemRequest(@NotNull UUID productId, @NotNull UUID warehouseLocationId,
        @Min(1) int quantity, @Size(max = 1000) String notes) {
}
