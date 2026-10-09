package com.warehousing.wmsapi.warehouse.repository;

import com.warehousing.wmsapi.warehouse.dto.WarehouseResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WarehouseQueryRepository {
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "code", "w.code", "name", "w.name", "created_at", "w.created_at");
    private static final String VISIBLE_WAREHOUSES = """
            FROM warehouses w
            JOIN users u ON u.email = ? AND u.deleted_at IS NULL AND u.is_active
            WHERE w.deleted_at IS NULL AND (
                EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                        WHERE ur.user_id = u.id AND ur.deleted_at IS NULL
                        AND r.deleted_at IS NULL AND r.name = 'SUPERADMIN')
                OR EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id
                        AND uw.warehouse_id = w.id AND uw.deleted_at IS NULL)
            )
            """;
    private final JdbcTemplate jdbcTemplate;

    public List<WarehouseResponse> findVisible(String email, String search, String sortColumn,
                                                String sortDirection, int limit, long offset) {
        String column = SORT_COLUMNS.get(sortColumn);
        if (column == null || !("ASC".equals(sortDirection) || "DESC".equals(sortDirection))) {
            throw new IllegalArgumentException("Unsupported warehouse query ordering.");
        }
        String searchFilter = search == null ? "" : " AND (w.code ILIKE ? OR w.name ILIKE ? OR COALESCE(w.address, '') ILIKE ?)";
        List<Object> args = new ArrayList<>();
        args.add(email);
        if (search != null) {
            args.add(search); args.add(search); args.add(search);
        }
        args.add(limit); args.add(offset);
        String sql = """
                SELECT w.id, w.code, w.name, w.address, w.is_active, w.created_at
                """ + VISIBLE_WAREHOUSES + searchFilter + " ORDER BY " + column + " " + sortDirection
                + ", w.id LIMIT ? OFFSET ?";
        List<WarehouseResponse> content = jdbcTemplate.query(sql,
                (row, index) -> new WarehouseResponse(row.getObject("id", UUID.class), row.getString("code"),
                        row.getString("name"), row.getString("address"), row.getBoolean("is_active"),
                        row.getObject("created_at", OffsetDateTime.class)), args.toArray());
        return content;
    }

    public long countVisible(String email, String search) {
        String searchFilter = search == null ? "" : " AND (w.code ILIKE ? OR w.name ILIKE ? OR COALESCE(w.address, '') ILIKE ?)";
        List<Object> args = new ArrayList<>();
        args.add(email);
        if (search != null) { args.add(search); args.add(search); args.add(search); }
        Long total = jdbcTemplate.queryForObject("SELECT count(*) " + VISIBLE_WAREHOUSES + searchFilter,
                Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    public boolean isReferenced(UUID id) {
        Boolean referenced = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM warehouse_locations WHERE warehouse_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM user_warehouses WHERE warehouse_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM inbounds WHERE warehouse_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM outbounds WHERE warehouse_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM stock_transfers WHERE warehouse_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM stock_adjustments WHERE warehouse_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM warehouse_location_items WHERE warehouse_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM stock_movements WHERE warehouse_id = ? AND deleted_at IS NULL
                )
                """, Boolean.class, id, id, id, id, id, id, id, id);
        return Boolean.TRUE.equals(referenced);
    }
}
