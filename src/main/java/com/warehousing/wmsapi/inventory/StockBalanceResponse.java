package com.warehousing.wmsapi.inventory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StockBalanceResponse(UUID id, UUID warehouseId, String warehouseCode, String warehouseName,
        UUID warehouseLocationId, String locationCode, String locationName, UUID productId, String sku,
        String productName, String unit, int quantity, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
