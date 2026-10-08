package com.warehousing.wmsapi.inventory;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StockBalanceRepository {
    private final JdbcTemplate jdbcTemplate;

    public StockBalanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Balance lockOrCreate(UUID warehouseId, UUID locationId, UUID productId) {
        jdbcTemplate.update("""
                INSERT INTO warehouse_location_items(warehouse_id, warehouse_location_id, product_id, quantity)
                VALUES (?, ?, ?, 0)
                ON CONFLICT (warehouse_location_id, product_id) DO UPDATE
                    SET quantity = CASE WHEN warehouse_location_items.deleted_at IS NULL
                                        THEN warehouse_location_items.quantity ELSE 0 END,
                        deleted_at = NULL, updated_at = CURRENT_TIMESTAMP
                """, warehouseId, locationId, productId);
        return lock(locationId, productId);
    }

    public Balance lock(UUID locationId, UUID productId) {
        List<Balance> balances = jdbcTemplate.query("""
                SELECT id, warehouse_id, warehouse_location_id, product_id, quantity
                FROM warehouse_location_items
                WHERE warehouse_location_id = ? AND product_id = ? AND deleted_at IS NULL
                FOR UPDATE
                """, (row, index) -> new Balance(row.getObject("id", UUID.class),
                row.getObject("warehouse_id", UUID.class), row.getObject("warehouse_location_id", UUID.class),
                row.getObject("product_id", UUID.class), row.getInt("quantity")), locationId, productId);
        return balances.isEmpty() ? null : balances.get(0);
    }

    public int updateQuantity(UUID id, int quantity) {
        return jdbcTemplate.update("""
                UPDATE warehouse_location_items SET quantity = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted_at IS NULL
                """, quantity, id);
    }

    public record Balance(UUID id, UUID warehouseId, UUID locationId, UUID productId, int quantity) {
    }
}
