package com.warehousing.wmsapi.inbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InboundUpdateRequest(
        @Size(max = 100) String purchaseOrderNumber,
        @Size(max = 5000) String notes,
        @NotEmpty List<@Valid InboundItemRequest> items
) {
}
