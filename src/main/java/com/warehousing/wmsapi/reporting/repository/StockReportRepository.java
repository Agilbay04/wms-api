package com.warehousing.wmsapi.reporting.repository;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import com.warehousing.wmsapi.inventory.StockBalanceResponse;
import com.warehousing.wmsapi.inventory.StockMovementResponse;
import com.warehousing.wmsapi.reporting.dto.StockReportRequest;
import com.warehousing.wmsapi.reporting.dto.StockMovementReportRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class StockReportRepository {
    private static final String ACCESSIBLE = """
            JOIN users u ON u.email = ? AND u.deleted_at IS NULL AND u.is_active
            WHERE (EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                          WHERE ur.user_id = u.id AND ur.deleted_at IS NULL
                            AND r.deleted_at IS NULL AND r.name = 'SUPERADMIN')
               OR EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id
                          AND uw.warehouse_id = w.id AND uw.deleted_at IS NULL))
            """;
    private static final Map<String, String> STOCK_SORTS = Map.of("created_at", "wli.created_at",
            "updated_at", "wli.updated_at", "quantity", "wli.quantity", "sku", "p.sku",
            "location_code", "wl.code");
    private static final Map<String, String> MOVEMENT_SORTS = Map.of("created_at", "sm.occurred_at",
            "occurred_at", "sm.occurred_at",
            "quantity", "sm.quantity", "stock_after", "sm.stock_after", "sku", "p.sku",
            "location_code", "wl.code");

    private final JdbcTemplate jdbcTemplate;
    

    @Transactional(readOnly = true)
    public PageResponse<StockBalanceResponse> stocks(Authentication authentication, StockReportRequest request) {
        String sortColumn = validate(request, STOCK_SORTS);
        List<Object> args = new ArrayList<>();
        args.add(authentication.getName());
        String filter = warehouseFilter(request.getWarehouseId(), "w.id", args);
        String pattern = searchPattern(request.getSearch());
        if (pattern != null) {
            filter += " AND (p.sku ILIKE ? OR p.name ILIKE ? OR wl.code ILIKE ? OR w.code ILIKE ?)";
            args.add(pattern); args.add(pattern); args.add(pattern); args.add(pattern);
        }
        String fromWhere = """
                FROM warehouse_location_items wli
                JOIN warehouse_locations wl ON wl.id = wli.warehouse_location_id AND wl.deleted_at IS NULL
                JOIN warehouses w ON w.id = wli.warehouse_id AND w.deleted_at IS NULL
                JOIN products p ON p.id = wli.product_id AND p.deleted_at IS NULL
                """ + ACCESSIBLE + " AND wli.deleted_at IS NULL" + filter;
        long total = count(fromWhere, args);
        args.add(request.getSize()); args.add((long) (request.getPage() - 1) * request.getSize());
        String sql = """
                SELECT wli.id, w.id warehouse_id, w.code warehouse_code, w.name warehouse_name,
                    wl.id warehouse_location_id, wl.code location_code, wl.name location_name,
                    p.id product_id, p.sku, p.name product_name, p.unit, wli.quantity,
                    wli.created_at, wli.updated_at
                """ + fromWhere + " ORDER BY " + sortColumn + " " + MasterPage.parseDirection(request.getOrder())
                + ", wli.id LIMIT ? OFFSET ?";
        List<StockBalanceResponse> rows = jdbcTemplate.query(sql, (row, index) -> new StockBalanceResponse(
                row.getObject("id", UUID.class), row.getObject("warehouse_id", UUID.class),
                row.getString("warehouse_code"), row.getString("warehouse_name"),
                row.getObject("warehouse_location_id", UUID.class), row.getString("location_code"),
                row.getString("location_name"), row.getObject("product_id", UUID.class), row.getString("sku"),
                row.getString("product_name"), row.getString("unit"), row.getInt("quantity"),
                row.getObject("created_at", OffsetDateTime.class), row.getObject("updated_at", OffsetDateTime.class)),
                args.toArray());
        return page(request, rows, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> movements(Authentication authentication, StockMovementReportRequest request) {
        String sortColumn = validate(request, MOVEMENT_SORTS);
        validateDates(request.getFromDate(), request.getToDate());
        List<Object> args = new ArrayList<>();
        args.add(authentication.getName());
        String filter = warehouseFilter(request.getWarehouseId(), "w.id", args);
        filter += dateFilter(request.getFromDate(), request.getToDate(), "sm.occurred_at", args);
        String pattern = searchPattern(request.getSearch());
        if (pattern != null) {
            filter += " AND (p.sku ILIKE ? OR p.name ILIKE ? OR wl.code ILIKE ? OR sm.source_entity_type ILIKE ?)";
            args.add(pattern); args.add(pattern); args.add(pattern); args.add(pattern);
        }
        String fromWhere = """
                FROM stock_movements sm
                JOIN warehouse_locations wl ON wl.id = sm.warehouse_location_id AND wl.deleted_at IS NULL
                JOIN warehouses w ON w.id = sm.warehouse_id AND w.deleted_at IS NULL
                JOIN products p ON p.id = sm.product_id AND p.deleted_at IS NULL
                """ + ACCESSIBLE + " AND sm.deleted_at IS NULL" + filter;
        long total = count(fromWhere, args);
        args.add(request.getSize()); args.add((long) (request.getPage() - 1) * request.getSize());
        String sql = """
                SELECT sm.id, w.id warehouse_id, w.code warehouse_code, w.name warehouse_name,
                    wl.id warehouse_location_id, wl.code location_code, p.id product_id, p.sku,
                    p.name product_name, sm.movement_type, sm.movement_direction, sm.quantity,
                    sm.stock_after, sm.source_entity_type, sm.source_entity_id, sm.source_item_id,
                    sm.occurred_at, sm.created_by_user_id
                """ + fromWhere + " ORDER BY " + sortColumn + " " + MasterPage.parseDirection(request.getOrder())
                + ", sm.id LIMIT ? OFFSET ?";
        List<StockMovementResponse> rows = jdbcTemplate.query(sql, (row, index) -> new StockMovementResponse(
                row.getObject("id", UUID.class), row.getObject("warehouse_id", UUID.class),
                row.getString("warehouse_code"), row.getString("warehouse_name"),
                row.getObject("warehouse_location_id", UUID.class), row.getString("location_code"),
                row.getObject("product_id", UUID.class), row.getString("sku"), row.getString("product_name"),
                row.getString("movement_type"), row.getString("movement_direction"), row.getInt("quantity"),
                row.getInt("stock_after"), row.getString("source_entity_type"),
                row.getObject("source_entity_id", UUID.class), row.getObject("source_item_id", UUID.class),
                row.getObject("occurred_at", OffsetDateTime.class), row.getObject("created_by_user_id", UUID.class)),
                args.toArray());
        return page(request, rows, total);
    }

    private String validate(com.warehousing.wmsapi.common.pagination.BasePageRequest request, Map<String, String> sortFields) {
        if (request.getPage() == null || request.getSize() == null || request.getPage() < 1
                || request.getSize() < 1 || request.getSize() > 100 || request.getSort() == null
                || !sortFields.containsKey(request.getSort())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported report sort field.");
        }
        return sortFields.get(request.getSort());
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "from_date must be earlier than or equal to to_date.");
        }
    }

    private String warehouseFilter(UUID warehouseId, String column, List<Object> args) {
        if (warehouseId == null) return "";
        args.add(warehouseId);
        return " AND " + column + " = ?";
    }

    private String dateFilter(LocalDate from, LocalDate to, String column, List<Object> args) {
        String filter = "";
        if (from != null) { filter += " AND " + column + " >= ?"; args.add(from.atStartOfDay().atOffset(ZoneOffset.UTC)); }
        if (to != null) { filter += " AND " + column + " < ?"; args.add(to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)); }
        return filter;
    }

    private String searchPattern(String search) {
        String normalized = MasterPage.normalizeSearch(search);
        return normalized == null ? null : "%" + normalized + "%";
    }

    private long count(String fromWhere, List<Object> args) {
        Long total = jdbcTemplate.queryForObject("SELECT count(*) " + fromWhere, Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    private <T> PageResponse<T> page(com.warehousing.wmsapi.common.pagination.BasePageRequest request, List<T> content, long total) {
        MasterPage.parseDirection(request.getOrder());
        int pages = (int) ((total + request.getSize() - 1) / request.getSize());
        return new PageResponse<>(content, request.getPage(), request.getSize(), total, pages);
    }
}
