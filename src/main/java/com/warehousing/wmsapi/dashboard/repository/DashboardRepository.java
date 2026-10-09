package com.warehousing.wmsapi.dashboard.repository;

import com.warehousing.wmsapi.dashboard.dto.DashboardResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardRepository {
    private final JdbcTemplate jdbcTemplate;

    @Cacheable(cacheNames = "dashboards", key = "#warehouseId")
    public DashboardResponse get(UUID warehouseId) {
        DashboardCounts counts = jdbcTemplate.queryForObject("""
                WITH product_stock AS (
                    SELECT p.id, p.minimum_stock, COALESCE(SUM(wli.quantity), 0) quantity
                    FROM products p LEFT JOIN warehouse_location_items wli
                        ON wli.product_id = p.id AND wli.warehouse_id = ? AND wli.deleted_at IS NULL
                    WHERE p.deleted_at IS NULL AND p.is_active
                    GROUP BY p.id, p.minimum_stock
                )
                SELECT (SELECT count(*) FROM products WHERE deleted_at IS NULL AND is_active) total_products,
                    COALESCE((SELECT sum(quantity) FROM product_stock), 0) total_stock,
                    (SELECT count(*) FROM product_stock WHERE quantity > 0 AND quantity <= minimum_stock) low_stock,
                    (SELECT count(*) FROM product_stock WHERE quantity = 0) out_of_stock
                """, (row, index) -> new DashboardCounts(row.getLong("total_products"),
                row.getLong("total_stock"), row.getLong("low_stock"), row.getLong("out_of_stock")), warehouseId);
        Map<String, Long> pending = new LinkedHashMap<>();
        pending.put("inbounds", pendingCount("inbounds", warehouseId));
        pending.put("outbounds", pendingCount("outbounds", warehouseId));
        pending.put("stock_transfers", pendingCount("stock_transfers", warehouseId));
        pending.put("stock_adjustments", pendingCount("stock_adjustments", warehouseId));
        return new DashboardResponse(warehouseId, counts.totalProducts(), counts.totalStock(), counts.lowStock(),
                counts.outOfStock(), Map.copyOf(pending));
    }

    private long pendingCount(String table, UUID warehouseId) {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table
                + " WHERE warehouse_id = ? AND status = 'PENDING' AND submitted_at IS NOT NULL AND deleted_at IS NULL",
                Long.class, warehouseId);
        return count == null ? 0 : count;
    }

    private record DashboardCounts(long totalProducts, long totalStock, long lowStock, long outOfStock) { }
}
