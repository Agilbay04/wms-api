package com.warehousing.wmsapi.adjustment.repository;

import com.warehousing.wmsapi.adjustment.StockAdjustmentStatus;
import com.warehousing.wmsapi.adjustment.StockAdjustmentType;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentItemResponse;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentListItemResponse;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StockAdjustmentRepository {
    private static final Set<String> SORT_FIELDS = Set.of("created_at", "reference_number", "status", "adjustment_type");
    private static final String VISIBLE_ADJUSTMENTS = """
            FROM stock_adjustments sa
            JOIN users u ON u.email = ? AND u.deleted_at IS NULL AND u.is_active
            WHERE sa.deleted_at IS NULL AND (EXISTS (
                SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = u.id AND ur.deleted_at IS NULL AND r.deleted_at IS NULL AND r.name = 'SUPERADMIN')
                OR EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id
                AND uw.warehouse_id = sa.warehouse_id AND uw.deleted_at IS NULL))
            """;
    private static final RowMapper<Header> HEADER_MAPPER = (row, index) -> new Header(
            row.getObject("id", UUID.class), row.getObject("warehouse_id", UUID.class),
            row.getString("reference_number"), StockAdjustmentStatus.valueOf(row.getString("status")),
            StockAdjustmentType.valueOf(row.getString("adjustment_type")), row.getString("reason"),
            row.getString("rejection_note"), row.getObject("submitted_at", OffsetDateTime.class),
            row.getObject("reviewed_at", OffsetDateTime.class), row.getObject("created_by_user_id", UUID.class),
            row.getObject("reviewed_by_user_id", UUID.class), row.getObject("created_at", OffsetDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public StockAdjustmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID activeUserId(String email) {
        List<UUID> ids = jdbcTemplate.query("""
                SELECT id FROM users WHERE email = ? AND deleted_at IS NULL AND is_active
                """, (row, index) -> row.getObject("id", UUID.class), email);
        if (ids.isEmpty()) throw new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Current user was not found.");
        return ids.get(0);
    }

    public boolean isActiveProduct(UUID productId) {
        Boolean active = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM products WHERE id = ? AND deleted_at IS NULL AND is_active)
                """, Boolean.class, productId);
        return Boolean.TRUE.equals(active);
    }

    public boolean isActiveLocationInWarehouse(UUID locationId, UUID warehouseId) {
        Boolean active = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM warehouse_locations
                    WHERE id = ? AND warehouse_id = ? AND deleted_at IS NULL AND is_active)
                """, Boolean.class, locationId, warehouseId);
        return Boolean.TRUE.equals(active);
    }

    public UUID insert(UUID warehouseId, String reference, StockAdjustmentType type, String reason, UUID creatorId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO stock_adjustments(warehouse_id, reference_number, status, adjustment_type, reason,
                    created_by_user_id) VALUES (?, ?, 'PENDING', ?, ?, ?) RETURNING id
                """, UUID.class, warehouseId, reference, type.name(), reason, creatorId);
    }

    public void insertItems(UUID adjustmentId, List<ItemInput> items) {
        for (ItemInput item : items) {
            jdbcTemplate.update("""
                    INSERT INTO stock_adjustment_items(stock_adjustment_id, product_id, warehouse_location_id,
                        quantity_change, notes) VALUES (?, ?, ?, ?, ?)
                    """, adjustmentId, item.productId(), item.locationId(), item.quantityChange(), item.notes());
        }
    }

    public void replaceItems(UUID adjustmentId, List<ItemInput> items) {
        jdbcTemplate.update("DELETE FROM stock_adjustment_items WHERE stock_adjustment_id = ?", adjustmentId);
        insertItems(adjustmentId, items);
    }

    public Optional<Header> findHeader(UUID id) {
        List<Header> rows = jdbcTemplate.query("""
                SELECT id, warehouse_id, reference_number, status, adjustment_type, reason, rejection_note,
                    submitted_at, reviewed_at, created_by_user_id, reviewed_by_user_id, created_at
                FROM stock_adjustments WHERE id = ? AND deleted_at IS NULL
                """, HEADER_MAPPER, id);
        return rows.stream().findFirst();
    }

    public Header lockHeader(UUID id) {
        List<Header> rows = jdbcTemplate.query("""
                SELECT id, warehouse_id, reference_number, status, adjustment_type, reason, rejection_note,
                    submitted_at, reviewed_at, created_by_user_id, reviewed_by_user_id, created_at
                FROM stock_adjustments WHERE id = ? AND deleted_at IS NULL FOR UPDATE
                """, HEADER_MAPPER, id);
        if (rows.isEmpty()) throw notFound();
        return rows.get(0);
    }

    public List<ItemRow> findItems(UUID adjustmentId) {
        return jdbcTemplate.query("""
                SELECT sai.id, sai.product_id, p.sku, p.name, sai.warehouse_location_id,
                    wl.code location_code, sai.quantity_change, sai.notes
                FROM stock_adjustment_items sai JOIN products p ON p.id = sai.product_id
                JOIN warehouse_locations wl ON wl.id = sai.warehouse_location_id
                WHERE sai.stock_adjustment_id = ? AND sai.deleted_at IS NULL ORDER BY sai.created_at, sai.id
                """, (row, index) -> new ItemRow(row.getObject("id", UUID.class),
                row.getObject("product_id", UUID.class), row.getString("sku"), row.getString("name"),
                row.getObject("warehouse_location_id", UUID.class), row.getString("location_code"),
                row.getInt("quantity_change"), row.getString("notes")), adjustmentId);
    }

    public int itemCount(UUID id) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM stock_adjustment_items WHERE stock_adjustment_id = ? AND deleted_at IS NULL
                """, Integer.class, id);
        return count == null ? 0 : count;
    }

    public void updateHeader(UUID id, StockAdjustmentType type, String reason) {
        jdbcTemplate.update("""
                UPDATE stock_adjustments SET adjustment_type = ?, reason = ?, status = 'PENDING', rejection_note = NULL,
                    submitted_at = NULL, reviewed_at = NULL, reviewed_by_user_id = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, type.name(), reason, id);
    }

    public void submit(UUID id) {
        jdbcTemplate.update("UPDATE stock_adjustments SET submitted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                id);
    }

    public void approve(UUID id, UUID reviewerId) {
        jdbcTemplate.update("""
                UPDATE stock_adjustments SET status = 'APPROVED', reviewed_by_user_id = ?,
                    reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, reviewerId, id);
    }

    public void reject(UUID id, UUID reviewerId, String note) {
        jdbcTemplate.update("""
                UPDATE stock_adjustments SET status = 'REJECTED', rejection_note = ?, reviewed_by_user_id = ?,
                    reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, note, reviewerId, id);
    }

    public void softDelete(UUID id) {
        jdbcTemplate.update("""
                UPDATE stock_adjustment_items SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE stock_adjustment_id = ? AND deleted_at IS NULL
                """, id);
        jdbcTemplate.update("""
                UPDATE stock_adjustments SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, id);
    }

    public boolean hasMovements(UUID id) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM stock_movements WHERE source_entity_type = 'STOCK_ADJUSTMENT'
                    AND source_entity_id = ? AND deleted_at IS NULL)
                """, Boolean.class, id);
        return Boolean.TRUE.equals(exists);
    }

    public void audit(UUID userId, String action, UUID id, String description) {
        jdbcTemplate.update("""
                INSERT INTO audit_trails(user_id, action, entity_type, entity_id, description)
                VALUES (?, ?, 'STOCK_ADJUSTMENT', ?, ?)
                """, userId, action, id, description);
    }

    public PageResponse<StockAdjustmentListItemResponse> list(String email, BasePageRequest request) {
        if (request.getPage() < 1 || request.getSize() < 1 || request.getSize() > 100
                || !SORT_FIELDS.contains(request.getSort())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported stock adjustment sort field.");
        }
        String sortColumn = switch (request.getSort()) {
            case "reference_number" -> "sa.reference_number";
            case "status" -> "sa.status";
            case "adjustment_type" -> "sa.adjustment_type";
            default -> "sa.created_at";
        };
        String direction = MasterPage.parseDirection(request.getOrder()).name();
        String search = MasterPage.normalizeSearch(request.getSearch());
        String pattern = search == null ? null : "%" + search + "%";
        String filter = pattern == null ? "" : " AND (sa.reference_number ILIKE ? OR sa.reason ILIKE ? "
                + "OR sa.adjustment_type ILIKE ?)";
        List<Object> args = new ArrayList<>();
        args.add(email);
        if (pattern != null) { args.add(pattern); args.add(pattern); args.add(pattern); }
        Long count = jdbcTemplate.queryForObject("SELECT count(*) " + VISIBLE_ADJUSTMENTS + filter, Long.class,
                args.toArray());
        args.add(request.getSize());
        args.add((long) (request.getPage() - 1) * request.getSize());
        String sql = """
                SELECT sa.id, sa.warehouse_id, sa.reference_number, sa.status, sa.adjustment_type, sa.reason,
                    sa.rejection_note, sa.submitted_at, sa.reviewed_at, sa.created_by_user_id,
                    sa.reviewed_by_user_id, sa.created_at,
                    (SELECT count(*) FROM stock_adjustment_items sai WHERE sai.stock_adjustment_id = sa.id
                        AND sai.deleted_at IS NULL) item_count
                """ + VISIBLE_ADJUSTMENTS + filter + " ORDER BY " + sortColumn + " " + direction
                + ", sa.id LIMIT ? OFFSET ?";
        List<StockAdjustmentListItemResponse> content = jdbcTemplate.query(sql, (row, index) ->
                new StockAdjustmentListItemResponse(row.getObject("id", UUID.class),
                        row.getObject("warehouse_id", UUID.class), row.getString("reference_number"),
                        StockAdjustmentStatus.valueOf(row.getString("status")),
                        StockAdjustmentType.valueOf(row.getString("adjustment_type")), row.getString("reason"),
                        row.getString("rejection_note"), row.getObject("submitted_at", OffsetDateTime.class),
                        row.getObject("reviewed_at", OffsetDateTime.class),
                        row.getObject("created_by_user_id", UUID.class),
                        row.getObject("reviewed_by_user_id", UUID.class),
                        row.getObject("created_at", OffsetDateTime.class), row.getInt("item_count")), args.toArray());
        long total = count == null ? 0 : count;
        int pages = (int) ((total + request.getSize() - 1) / request.getSize());
        return new PageResponse<>(content, request.getPage(), request.getSize(), total, pages);
    }

    public StockAdjustmentResponse response(Header header) {
        List<StockAdjustmentItemResponse> items = findItems(header.id()).stream()
                .map(item -> new StockAdjustmentItemResponse(item.id(), item.productId(), item.productSku(),
                        item.productName(), item.locationId(), item.locationCode(), item.quantityChange(), item.notes()))
                .toList();
        return new StockAdjustmentResponse(header.id(), header.warehouseId(), header.referenceNumber(),
                header.status(), header.adjustmentType(), header.reason(), header.rejectionNote(),
                header.submittedAt(), header.reviewedAt(), header.createdByUserId(), header.reviewedByUserId(),
                header.createdAt(), items);
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "STOCK_ADJUSTMENT_NOT_FOUND",
                "Stock adjustment was not found.");
    }

    public record Header(UUID id, UUID warehouseId, String referenceNumber, StockAdjustmentStatus status,
            StockAdjustmentType adjustmentType, String reason, String rejectionNote, OffsetDateTime submittedAt,
            OffsetDateTime reviewedAt, UUID createdByUserId, UUID reviewedByUserId, OffsetDateTime createdAt) { }
    public record ItemInput(UUID productId, UUID locationId, int quantityChange, String notes) { }
    public record ItemRow(UUID id, UUID productId, String productSku, String productName, UUID locationId,
            String locationCode, int quantityChange, String notes) { }
}
