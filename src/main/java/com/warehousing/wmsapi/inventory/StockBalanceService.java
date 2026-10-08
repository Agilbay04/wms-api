package com.warehousing.wmsapi.inventory;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockBalanceService {
    private final JdbcTemplate jdbcTemplate;

    public StockBalanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void lockOrCreate(UUID warehouseId, UUID locationId, UUID productId) {
        jdbcTemplate.update("""
                INSERT INTO warehouse_location_items(warehouse_id, warehouse_location_id, product_id, quantity)
                VALUES (?, ?, ?, 0)
                ON CONFLICT (warehouse_location_id, product_id) DO UPDATE
                    SET quantity = CASE WHEN warehouse_location_items.deleted_at IS NULL
                                        THEN warehouse_location_items.quantity ELSE 0 END,
                        deleted_at = NULL, updated_at = CURRENT_TIMESTAMP
                """, warehouseId, locationId, productId);
        jdbcTemplate.queryForObject("""
                SELECT id FROM warehouse_location_items
                WHERE warehouse_location_id = ? AND product_id = ? FOR UPDATE
                """, UUID.class, locationId, productId);
    }

    public int add(UUID locationId, UUID productId, int quantity) {
        jdbcTemplate.update("""
                UPDATE warehouse_location_items SET quantity = quantity + ?, updated_at = CURRENT_TIMESTAMP
                WHERE warehouse_location_id = ? AND product_id = ? AND deleted_at IS NULL
                """, quantity, locationId, productId);
        Integer balance = jdbcTemplate.queryForObject("""
                SELECT quantity FROM warehouse_location_items
                WHERE warehouse_location_id = ? AND product_id = ? AND deleted_at IS NULL
                """, Integer.class, locationId, productId);
        return balance == null ? 0 : balance;
    }
}
