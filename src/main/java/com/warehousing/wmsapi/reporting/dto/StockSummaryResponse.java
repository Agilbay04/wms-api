package com.warehousing.wmsapi.reporting.dto;

import java.util.UUID;

public record StockSummaryResponse(UUID warehouseId, long totalProducts, long totalStock,
        long lowStockProducts, long outOfStockProducts) {
}
