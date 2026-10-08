package com.warehousing.wmsapi.reporting.repository;

import com.warehousing.wmsapi.reporting.dto.StockSummaryResponse;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockSummaryRepository {
    private final JdbcTemplate jdbcTemplate;

    public StockSummaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable(cacheNames = "stock-summaries", key = "#warehouseId")
    public StockSummaryResponse get(UUID warehouseId) {
        return jdbcTemplate.queryForObject("""
                WITH product_stock AS (
                    SELECT p.id, p.minimum_stock, COALESCE(SUM(wli.quantity), 0) quantity
                    FROM products p LEFT JOIN warehouse_location_items wli
                        ON wli.product_id = p.id AND wli.warehouse_id = ? AND wli.deleted_at IS NULL
                    WHERE p.deleted_at IS NULL AND p.is_active
                    GROUP BY p.id, p.minimum_stock
                )
                SELECT (SELECT count(*) FROM product_stock) total_products,
                    COALESCE((SELECT sum(quantity) FROM product_stock), 0) total_stock,
                    (SELECT count(*) FROM product_stock WHERE quantity > 0 AND quantity <= minimum_stock) low_stock,
                    (SELECT count(*) FROM product_stock WHERE quantity = 0) out_of_stock
                """, (row, index) -> new StockSummaryResponse(warehouseId, row.getLong("total_products"),
                row.getLong("total_stock"), row.getLong("low_stock"), row.getLong("out_of_stock")), warehouseId);
    }
}
