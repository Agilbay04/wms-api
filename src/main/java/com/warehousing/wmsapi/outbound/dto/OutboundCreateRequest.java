package com.warehousing.wmsapi.outbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record OutboundCreateRequest(@NotNull UUID warehouseId, @Size(max = 5000) String notes,
        @NotEmpty List<@Valid OutboundItemRequest> items) {
}
