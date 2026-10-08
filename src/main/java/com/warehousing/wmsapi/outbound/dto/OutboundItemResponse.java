package com.warehousing.wmsapi.outbound.dto;

import java.util.UUID;

public record OutboundItemResponse(UUID id, UUID productId, String productSku, String productName,
        UUID warehouseLocationId, String locationCode, int quantity, String notes) {
}
