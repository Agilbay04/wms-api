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
}
