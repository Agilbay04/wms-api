package com.warehousing.wmsapi.inbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record InboundCreateRequest(
        @NotNull UUID warehouseId,
        @Size(max = 100) String purchaseOrderNumber,
        @Size(max = 5000) String notes,
        @NotEmpty List<@Valid InboundItemRequest> items
) {
}
