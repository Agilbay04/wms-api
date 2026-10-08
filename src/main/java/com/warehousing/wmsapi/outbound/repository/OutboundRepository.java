package com.warehousing.wmsapi.outbound.repository;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import com.warehousing.wmsapi.outbound.enums.OutboundStatus;
import com.warehousing.wmsapi.outbound.dto.OutboundItemResponse;
import com.warehousing.wmsapi.outbound.dto.OutboundListItemResponse;
import com.warehousing.wmsapi.outbound.dto.OutboundResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboundRepository {
    private static final Set<String> SORT_FIELDS = Set.of("created_at", "reference_number", "status");
    private static final String VISIBLE_OUTBOUNDS = """
            FROM outbounds o
            JOIN users u ON u.email = ? AND u.deleted_at IS NULL AND u.is_active
            WHERE o.deleted_at IS NULL AND (EXISTS (
                SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = u.id AND ur.deleted_at IS NULL AND r.deleted_at IS NULL AND r.name = 'SUPERADMIN')
                OR EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id
                AND uw.warehouse_id = o.warehouse_id AND uw.deleted_at IS NULL))
            """;

    private final JdbcTemplate jdbcTemplate;

    public OutboundRepository(JdbcTemplate jdbcTemplate) {
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

    public UUID insert(UUID warehouseId, String reference, String notes, UUID creatorId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO outbounds(warehouse_id, reference_number, status, notes, created_by_user_id)
                VALUES (?, ?, 'PENDING', ?, ?) RETURNING id
                """, UUID.class, warehouseId, reference, notes, creatorId);
    }

    public void insertItems(UUID outboundId, List<ItemInput> items) {
        for (ItemInput item : items) {
            jdbcTemplate.update("""
                    INSERT INTO outbound_items(outbound_id, product_id, warehouse_location_id, quantity, notes)
                    VALUES (?, ?, ?, ?, ?)
                    """, outboundId, item.productId(), item.locationId(), item.quantity(), item.notes());
        }
    }

    public void replaceItems(UUID outboundId, List<ItemInput> items) {
        jdbcTemplate.update("DELETE FROM outbound_items WHERE outbound_id = ?", outboundId);
        insertItems(outboundId, items);
    }

    public Optional<Header> findHeader(UUID id) {
        List<Header> rows = jdbcTemplate.query("""
                SELECT id, warehouse_id, reference_number, status, notes, rejection_note, submitted_at, reviewed_at,
                    created_by_user_id, reviewed_by_user_id, created_at
                FROM outbounds WHERE id = ? AND deleted_at IS NULL
                """, (row, index) -> new Header(row.getObject("id", UUID.class),
                row.getObject("warehouse_id", UUID.class), row.getString("reference_number"),
                OutboundStatus.valueOf(row.getString("status")), row.getString("notes"),
                row.getString("rejection_note"), row.getObject("submitted_at", OffsetDateTime.class),
                row.getObject("reviewed_at", OffsetDateTime.class), row.getObject("created_by_user_id", UUID.class),
                row.getObject("reviewed_by_user_id", UUID.class), row.getObject("created_at", OffsetDateTime.class)), id);
        return rows.stream().findFirst();
    }

    public Header lockHeader(UUID id) {
        List<Header> rows = jdbcTemplate.query("""
                SELECT id, warehouse_id, reference_number, status, notes, rejection_note, submitted_at, reviewed_at,
                    created_by_user_id, reviewed_by_user_id, created_at
                FROM outbounds WHERE id = ? AND deleted_at IS NULL FOR UPDATE
                """, (row, index) -> new Header(row.getObject("id", UUID.class),
                row.getObject("warehouse_id", UUID.class), row.getString("reference_number"),
                OutboundStatus.valueOf(row.getString("status")), row.getString("notes"),
                row.getString("rejection_note"), row.getObject("submitted_at", OffsetDateTime.class),
                row.getObject("reviewed_at", OffsetDateTime.class), row.getObject("created_by_user_id", UUID.class),
                row.getObject("reviewed_by_user_id", UUID.class), row.getObject("created_at", OffsetDateTime.class)), id);
        if (rows.isEmpty()) throw notFound();
        return rows.get(0);
    }

    public List<ItemRow> findItems(UUID outboundId) {
        return jdbcTemplate.query("""
                SELECT oi.id, oi.product_id, p.sku, p.name, oi.warehouse_location_id, wl.code location_code,
                    oi.quantity, oi.notes
                FROM outbound_items oi JOIN products p ON p.id = oi.product_id
                JOIN warehouse_locations wl ON wl.id = oi.warehouse_location_id
                WHERE oi.outbound_id = ? AND oi.deleted_at IS NULL ORDER BY oi.created_at, oi.id
                """, (row, index) -> new ItemRow(row.getObject("id", UUID.class),
                row.getObject("product_id", UUID.class), row.getString("sku"), row.getString("name"),
                row.getObject("warehouse_location_id", UUID.class), row.getString("location_code"),
                row.getInt("quantity"), row.getString("notes")), outboundId);
    }

    public int itemCount(UUID id) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM outbound_items WHERE outbound_id = ? AND deleted_at IS NULL
                """, Integer.class, id);
        return count == null ? 0 : count;
    }

    public void updateHeader(UUID id, String notes) {
        jdbcTemplate.update("""
                UPDATE outbounds SET notes = ?, status = 'PENDING', rejection_note = NULL,
                    submitted_at = NULL, reviewed_at = NULL, reviewed_by_user_id = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, notes, id);
    }

    public void submit(UUID id) {
        jdbcTemplate.update("UPDATE outbounds SET submitted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                id);
    }

    public void approve(UUID id, UUID reviewerId) {
        jdbcTemplate.update("""
                UPDATE outbounds SET status = 'APPROVED', reviewed_by_user_id = ?,
                    reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, reviewerId, id);
    }

    public void reject(UUID id, UUID reviewerId, String note) {
        jdbcTemplate.update("""
                UPDATE outbounds SET status = 'REJECTED', rejection_note = ?, reviewed_by_user_id = ?,
                    reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, note, reviewerId, id);
    }

    public void softDelete(UUID id) {
        jdbcTemplate.update("""
                UPDATE outbound_items SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE outbound_id = ? AND deleted_at IS NULL
                """, id);
        jdbcTemplate.update("""
                UPDATE outbounds SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, id);
    }

    public boolean hasMovements(UUID id) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM stock_movements WHERE source_entity_type = 'OUTBOUND'
                    AND source_entity_id = ? AND deleted_at IS NULL)
                """, Boolean.class, id);
        return Boolean.TRUE.equals(exists);
    }

    public void audit(UUID userId, String action, UUID id, String description) {
        jdbcTemplate.update("""
                INSERT INTO audit_trails(user_id, action, entity_type, entity_id, description)
                VALUES (?, ?, 'OUTBOUND', ?, ?)
                """, userId, action, id, description);
    }

    public PageResponse<OutboundListItemResponse> list(String email, BasePageRequest request) {
        if (request.getPage() < 1 || request.getSize() < 1 || request.getSize() > 100
                || !SORT_FIELDS.contains(request.getSort())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported outbound sort field.");
        }
        String orderColumn = switch (request.getSort()) {
            case "reference_number" -> "o.reference_number";
            case "status" -> "o.status";
            default -> "o.created_at";
        };
        String direction = MasterPage.parseDirection(request.getOrder()).name();
        String search = MasterPage.normalizeSearch(request.getSearch());
        String pattern = search == null ? null : "%" + search + "%";
        String filter = pattern == null ? "" : " AND (o.reference_number ILIKE ? OR COALESCE(o.notes, '') ILIKE ?)";
        List<Object> args = new ArrayList<>();
        args.add(email);
        if (pattern != null) { args.add(pattern); args.add(pattern); }
        Long count = jdbcTemplate.queryForObject("SELECT count(*) " + VISIBLE_OUTBOUNDS + filter, Long.class,
                args.toArray());
        args.add(request.getSize());
        args.add((long) (request.getPage() - 1) * request.getSize());
        String sql = """
                SELECT o.id, o.warehouse_id, o.reference_number, o.status, o.notes, o.rejection_note,
                    o.submitted_at, o.reviewed_at, o.created_by_user_id, o.reviewed_by_user_id, o.created_at,
                    (SELECT count(*) FROM outbound_items oi WHERE oi.outbound_id = o.id AND oi.deleted_at IS NULL) item_count
                """ + VISIBLE_OUTBOUNDS + filter + " ORDER BY " + orderColumn + " " + direction + ", o.id LIMIT ? OFFSET ?";
        List<OutboundListItemResponse> content = jdbcTemplate.query(sql, (row, index) ->
                new OutboundListItemResponse(row.getObject("id", UUID.class),
                        row.getObject("warehouse_id", UUID.class), row.getString("reference_number"),
                        OutboundStatus.valueOf(row.getString("status")), row.getString("notes"),
                        row.getString("rejection_note"), row.getObject("submitted_at", OffsetDateTime.class),
                        row.getObject("reviewed_at", OffsetDateTime.class), row.getObject("created_by_user_id", UUID.class),
                        row.getObject("reviewed_by_user_id", UUID.class), row.getObject("created_at", OffsetDateTime.class),
                        row.getInt("item_count")), args.toArray());
        long total = count == null ? 0 : count;
        int pages = (int) ((total + request.getSize() - 1) / request.getSize());
        return new PageResponse<>(content, request.getPage(), request.getSize(), total, pages);
    }

    public OutboundResponse response(Header header) {
        List<OutboundItemResponse> items = findItems(header.id()).stream()
                .map(item -> new OutboundItemResponse(item.id(), item.productId(), item.productSku(), item.productName(),
                        item.locationId(), item.locationCode(), item.quantity(), item.notes()))
                .toList();
        return new OutboundResponse(header.id(), header.warehouseId(), header.referenceNumber(), header.status(),
                header.notes(), header.rejectionNote(), header.submittedAt(), header.reviewedAt(),
                header.createdByUserId(), header.reviewedByUserId(), header.createdAt(), items);
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "OUTBOUND_NOT_FOUND", "Outbound was not found.");
    }

    public record Header(UUID id, UUID warehouseId, String referenceNumber, OutboundStatus status, String notes,
            String rejectionNote, OffsetDateTime submittedAt, OffsetDateTime reviewedAt, UUID createdByUserId,
            UUID reviewedByUserId, OffsetDateTime createdAt) { }
    public record ItemInput(UUID productId, UUID locationId, int quantity, String notes) { }
    public record ItemRow(UUID id, UUID productId, String productSku, String productName, UUID locationId,
            String locationCode, int quantity, String notes) { }
}
