package com.warehousing.wmsapi.transfer.dto;

import java.util.UUID;

public record StockTransferItemResponse(UUID id, UUID productId, String productSku, String productName,
        UUID sourceWarehouseLocationId, String sourceLocationCode, UUID destinationWarehouseLocationId,
        String destinationLocationCode, int quantity, String notes) {
}
