package com.warehousing.wmsapi.inventory;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockQueryService {
    private static final String ACCESSIBLE_WAREHOUSES = """
            JOIN users u ON u.email = ? AND u.deleted_at IS NULL AND u.is_active
            WHERE (EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                          WHERE ur.user_id = u.id AND ur.deleted_at IS NULL
                            AND r.deleted_at IS NULL AND r.name = 'SUPERADMIN')
               OR EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id
                          AND uw.warehouse_id = w.id AND uw.deleted_at IS NULL))
            """;
    private static final Map<String, String> STOCK_SORTS = Map.of(
            "created_at", "wli.created_at", "updated_at", "wli.updated_at", "quantity", "wli.quantity",
            "sku", "p.sku", "location_code", "wl.code", "warehouse_code", "w.code");
    private static final Map<String, String> MOVEMENT_SORTS = Map.of(
            "created_at", "sm.occurred_at", "occurred_at", "sm.occurred_at", "quantity", "sm.quantity",
            "stock_after", "sm.stock_after", "sku", "p.sku", "location_code", "wl.code");

    private final JdbcTemplate jdbcTemplate;

    public StockQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public PageResponse<StockBalanceResponse> stocks(Authentication authentication, BasePageRequest request) {
        String sortColumn = sortColumn(request, STOCK_SORTS);
        String pattern = searchPattern(request);
        String searchClause = pattern == null ? "" : " AND (p.sku ILIKE ? OR p.name ILIKE ? "
                + "OR wl.code ILIKE ? OR w.code ILIKE ? OR w.name ILIKE ?)";
        List<Object> args = new ArrayList<>();
        args.add(authentication.getName());
        if (pattern != null) {
            for (int index = 0; index < 5; index++) args.add(pattern);
        }
        String fromWhere = """
                FROM warehouse_location_items wli
                JOIN warehouse_locations wl ON wl.id = wli.warehouse_location_id AND wl.deleted_at IS NULL
                JOIN warehouses w ON w.id = wli.warehouse_id AND w.deleted_at IS NULL
                JOIN products p ON p.id = wli.product_id AND p.deleted_at IS NULL
                """ + ACCESSIBLE_WAREHOUSES + " AND wli.deleted_at IS NULL" + searchClause;
        long total = count(fromWhere, args);
        args.add(request.getSize());
        args.add((long) (request.getPage() - 1) * request.getSize());
        String sql = """
                SELECT wli.id, w.id warehouse_id, w.code warehouse_code, w.name warehouse_name,
                    wl.id warehouse_location_id, wl.code location_code, wl.name location_name,
                    p.id product_id, p.sku, p.name product_name, p.unit, wli.quantity,
                    wli.created_at, wli.updated_at
                """ + fromWhere + " ORDER BY " + sortColumn + " " + MasterPage.parseDirection(request.getOrder())
                + ", wli.id LIMIT ? OFFSET ?";
        List<StockBalanceResponse> content = jdbcTemplate.query(sql, (row, index) -> new StockBalanceResponse(
                row.getObject("id", java.util.UUID.class), row.getObject("warehouse_id", java.util.UUID.class),
                row.getString("warehouse_code"), row.getString("warehouse_name"),
                row.getObject("warehouse_location_id", java.util.UUID.class), row.getString("location_code"),
                row.getString("location_name"), row.getObject("product_id", java.util.UUID.class),
                row.getString("sku"), row.getString("product_name"), row.getString("unit"), row.getInt("quantity"),
                row.getObject("created_at", OffsetDateTime.class), row.getObject("updated_at", OffsetDateTime.class)),
                args.toArray());
        return page(request, content, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> movements(Authentication authentication, BasePageRequest request) {
        String sortColumn = sortColumn(request, MOVEMENT_SORTS);
        String pattern = searchPattern(request);
        String searchClause = pattern == null ? "" : " AND (p.sku ILIKE ? OR p.name ILIKE ? "
                + "OR wl.code ILIKE ? OR w.code ILIKE ? OR w.name ILIKE ? OR sm.source_entity_type ILIKE ?)";
        List<Object> args = new ArrayList<>();
        args.add(authentication.getName());
        if (pattern != null) {
            for (int index = 0; index < 6; index++) args.add(pattern);
        }
        String fromWhere = """
                FROM stock_movements sm
                JOIN warehouse_locations wl ON wl.id = sm.warehouse_location_id AND wl.deleted_at IS NULL
                JOIN warehouses w ON w.id = sm.warehouse_id AND w.deleted_at IS NULL
                JOIN products p ON p.id = sm.product_id AND p.deleted_at IS NULL
                """ + ACCESSIBLE_WAREHOUSES + " AND sm.deleted_at IS NULL" + searchClause;
        long total = count(fromWhere, args);
        args.add(request.getSize());
        args.add((long) (request.getPage() - 1) * request.getSize());
        String sql = """
                SELECT sm.id, w.id warehouse_id, w.code warehouse_code, w.name warehouse_name,
                    wl.id warehouse_location_id, wl.code location_code, p.id product_id,
                    p.sku, p.name product_name, sm.movement_type, sm.movement_direction,
                    sm.quantity, sm.stock_after, sm.source_entity_type, sm.source_entity_id,
                    sm.source_item_id, sm.occurred_at, sm.created_by_user_id
                """ + fromWhere + " ORDER BY " + sortColumn + " " + MasterPage.parseDirection(request.getOrder())
                + ", sm.id LIMIT ? OFFSET ?";
        List<StockMovementResponse> content = jdbcTemplate.query(sql, (row, index) -> new StockMovementResponse(
                row.getObject("id", java.util.UUID.class), row.getObject("warehouse_id", java.util.UUID.class),
                row.getString("warehouse_code"), row.getString("warehouse_name"),
                row.getObject("warehouse_location_id", java.util.UUID.class), row.getString("location_code"),
                row.getObject("product_id", java.util.UUID.class), row.getString("sku"), row.getString("product_name"),
                row.getString("movement_type"), row.getString("movement_direction"), row.getInt("quantity"),
                row.getInt("stock_after"), row.getString("source_entity_type"),
                row.getObject("source_entity_id", java.util.UUID.class), row.getObject("source_item_id", java.util.UUID.class),
                row.getObject("occurred_at", OffsetDateTime.class), row.getObject("created_by_user_id", java.util.UUID.class)),
                args.toArray());
        return page(request, content, total);
    }

    private long count(String fromWhere, List<Object> args) {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) " + fromWhere, Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    private String sortColumn(BasePageRequest request, Map<String, String> sortFields) {
        if (request.getPage() == null || request.getSize() == null || request.getPage() < 1
                || request.getSize() < 1 || request.getSize() > 100 || request.getSort() == null
                || !sortFields.containsKey(request.getSort())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported inventory sort field.");
        }
        return sortFields.get(request.getSort());
    }

    private String searchPattern(BasePageRequest request) {
        String search = MasterPage.normalizeSearch(request.getSearch());
        return search == null ? null : "%" + search + "%";
    }

    private <T> PageResponse<T> page(BasePageRequest request, List<T> content, long total) {
        MasterPage.parseDirection(request.getOrder());
        int pages = (int) ((total + request.getSize() - 1) / request.getSize());
        return new PageResponse<>(content, request.getPage(), request.getSize(), total, pages);
    }
}
