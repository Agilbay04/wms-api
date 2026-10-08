package com.warehousing.wmsapi.adjustment.dto;

import java.util.UUID;

public record StockAdjustmentItemResponse(UUID id, UUID productId, String productSku, String productName,
        UUID warehouseLocationId, String locationCode, int quantityChange, String notes) {
}
