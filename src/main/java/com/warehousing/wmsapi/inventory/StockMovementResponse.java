package com.warehousing.wmsapi.inventory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StockMovementResponse(UUID id, UUID warehouseId, String warehouseCode, String warehouseName,
        UUID warehouseLocationId, String locationCode, UUID productId, String sku, String productName,
        String movementType, String movementDirection, int quantity, int stockAfter, String sourceEntityType,
        UUID sourceEntityId, UUID sourceItemId, OffsetDateTime occurredAt, UUID createdByUserId) {
}
