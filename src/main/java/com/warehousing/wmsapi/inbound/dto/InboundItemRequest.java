package com.warehousing.wmsapi.inbound.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InboundItemRequest(
        @NotNull UUID productId,
        @Min(1) int quantity,
        @Size(max = 1000) String notes
) {
}
