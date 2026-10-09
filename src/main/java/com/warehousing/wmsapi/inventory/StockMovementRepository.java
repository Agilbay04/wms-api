package com.warehousing.wmsapi.inventory;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StockMovementRepository {
    private final JdbcTemplate jdbcTemplate;

    public void insert(UUID warehouseId, UUID locationId, UUID productId, String movementType,
                       String direction, int quantity, int stockAfter, String sourceType,
                       UUID sourceId, UUID sourceItemId, UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO stock_movements(warehouse_id, warehouse_location_id, product_id,
                    movement_type, movement_direction, quantity, stock_after,
                    source_entity_type, source_entity_id, source_item_id, created_by_user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, warehouseId, locationId, productId, movementType, direction, quantity,
                stockAfter, sourceType, sourceId, sourceItemId, userId);
    }
}
