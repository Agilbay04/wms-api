package com.warehousing.wmsapi.reporting.dto;

import java.util.Map;
import java.util.UUID;

public record DashboardResponse(UUID warehouseId, long totalProducts, long totalStock,
        long lowStockProducts, long outOfStockProducts, Map<String, Long> pendingTransactions) {
}
