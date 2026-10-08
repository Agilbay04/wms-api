package com.warehousing.wmsapi.inventory;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockMovementService {
    private final JdbcTemplate jdbcTemplate;

    public StockMovementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordInbound(UUID warehouseId, UUID locationId, UUID productId, int quantity,
                              int stockAfter, UUID inboundId, UUID inboundItemId, UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO stock_movements(warehouse_id, warehouse_location_id, product_id,
                    movement_type, movement_direction, quantity, stock_after,
                    source_entity_type, source_entity_id, source_item_id, created_by_user_id)
                VALUES (?, ?, ?, 'INBOUND', 'IN', ?, ?, 'INBOUND', ?, ?, ?)
                """, warehouseId, locationId, productId, quantity, stockAfter,
                inboundId, inboundItemId, userId);
    }

    public void recordOutbound(UUID warehouseId, UUID locationId, UUID productId, int quantity,
                               int stockAfter, UUID sourceEntityId, UUID sourceItemId, UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO stock_movements(warehouse_id, warehouse_location_id, product_id,
                    movement_type, movement_direction, quantity, stock_after,
                    source_entity_type, source_entity_id, source_item_id, created_by_user_id)
                VALUES (?, ?, ?, 'OUTBOUND', 'OUT', ?, ?, 'OUTBOUND', ?, ?, ?)
                """, warehouseId, locationId, productId, quantity, stockAfter,
                sourceEntityId, sourceItemId, userId);
    }

    public void recordTransfer(UUID warehouseId, UUID locationId, UUID productId, String direction,
                               int quantity, int stockAfter, UUID transferId, UUID transferItemId, UUID userId) {
        String normalizedDirection = direction.toUpperCase(java.util.Locale.ROOT);
        if (!normalizedDirection.equals("IN") && !normalizedDirection.equals("OUT")) {
            throw new IllegalArgumentException("Transfer movement direction must be IN or OUT.");
        }
        jdbcTemplate.update("""
                INSERT INTO stock_movements(warehouse_id, warehouse_location_id, product_id,
                    movement_type, movement_direction, quantity, stock_after,
                    source_entity_type, source_entity_id, source_item_id, created_by_user_id)
                VALUES (?, ?, ?, 'STOCK_TRANSFER', ?, ?, ?, 'STOCK_TRANSFER', ?, ?, ?)
                """, warehouseId, locationId, productId, normalizedDirection, quantity, stockAfter,
                transferId, transferItemId, userId);
    }

    public void recordAdjustment(UUID warehouseId, UUID locationId, UUID productId, int quantityChange,
                                 int stockAfter, UUID adjustmentId, UUID adjustmentItemId, UUID userId) {
        if (quantityChange == 0 || quantityChange == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Adjustment movement quantity must be a nonzero supported integer.");
        }
        String direction = quantityChange > 0 ? "IN" : "OUT";
        jdbcTemplate.update("""
                INSERT INTO stock_movements(warehouse_id, warehouse_location_id, product_id,
                    movement_type, movement_direction, quantity, stock_after,
                    source_entity_type, source_entity_id, source_item_id, created_by_user_id)
                VALUES (?, ?, ?, 'STOCK_ADJUSTMENT', ?, ?, ?, 'STOCK_ADJUSTMENT', ?, ?, ?)
                """, warehouseId, locationId, productId, direction, Math.abs(quantityChange), stockAfter,
                adjustmentId, adjustmentItemId, userId);
    }
}
