package com.warehousing.wmsapi.inbound.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InboundPutawayItemRequest(
        @NotNull UUID inboundItemId,
        @NotNull UUID warehouseLocationId
) {
}
