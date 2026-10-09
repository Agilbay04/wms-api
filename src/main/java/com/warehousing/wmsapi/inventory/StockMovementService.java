package com.warehousing.wmsapi.inventory;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockMovementService {
    private final StockMovementRepository repository;

    public void recordInbound(UUID warehouseId, UUID locationId, UUID productId, int quantity,
                              int stockAfter, UUID inboundId, UUID inboundItemId, UUID userId) {
        repository.insert(warehouseId, locationId, productId, "INBOUND", "IN", quantity, stockAfter,
                "INBOUND", inboundId, inboundItemId, userId);
    }

    public void recordOutbound(UUID warehouseId, UUID locationId, UUID productId, int quantity,
                               int stockAfter, UUID sourceEntityId, UUID sourceItemId, UUID userId) {
        repository.insert(warehouseId, locationId, productId, "OUTBOUND", "OUT", quantity, stockAfter,
                "OUTBOUND", sourceEntityId, sourceItemId, userId);
    }

    public void recordTransfer(UUID warehouseId, UUID locationId, UUID productId, String direction,
                               int quantity, int stockAfter, UUID transferId, UUID transferItemId, UUID userId) {
        String normalizedDirection = direction.toUpperCase(java.util.Locale.ROOT);
        if (!normalizedDirection.equals("IN") && !normalizedDirection.equals("OUT")) {
            throw new IllegalArgumentException("Transfer movement direction must be IN or OUT.");
        }
        repository.insert(warehouseId, locationId, productId, "STOCK_TRANSFER", normalizedDirection,
                quantity, stockAfter, "STOCK_TRANSFER", transferId, transferItemId, userId);
    }

    public void recordAdjustment(UUID warehouseId, UUID locationId, UUID productId, int quantityChange,
                                 int stockAfter, UUID adjustmentId, UUID adjustmentItemId, UUID userId) {
        if (quantityChange == 0 || quantityChange == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Adjustment movement quantity must be a nonzero supported integer.");
        }
        String direction = quantityChange > 0 ? "IN" : "OUT";
        repository.insert(warehouseId, locationId, productId, "STOCK_ADJUSTMENT", direction,
                Math.abs(quantityChange), stockAfter, "STOCK_ADJUSTMENT", adjustmentId, adjustmentItemId, userId);
    }
}
