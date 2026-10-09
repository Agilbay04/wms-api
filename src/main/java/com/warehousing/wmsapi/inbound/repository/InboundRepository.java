package com.warehousing.wmsapi.inbound.repository;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import com.warehousing.wmsapi.inbound.dto.InboundItemResponse;
import com.warehousing.wmsapi.inbound.dto.InboundListItemResponse;
import com.warehousing.wmsapi.inbound.dto.InboundResponse;
import com.warehousing.wmsapi.inbound.enums.InboundStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InboundRepository {
    private static final Set<String> SORT_FIELDS = Set.of("created_at", "reference_number", "status");
    private static final String VISIBLE_INBOUNDS = """
            FROM inbounds i
            JOIN users u ON u.email = ? AND u.deleted_at IS NULL AND u.is_active
            WHERE i.deleted_at IS NULL AND (
                EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                        WHERE ur.user_id = u.id AND ur.deleted_at IS NULL
                        AND r.deleted_at IS NULL AND r.name = 'SUPERADMIN')
                OR EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id
                        AND uw.warehouse_id = i.warehouse_id AND uw.deleted_at IS NULL)
            )
            """;
    private static final RowMapper<Header> HEADER_MAPPER = (row, index) -> new Header(
            row.getObject("id", UUID.class),
            row.getObject("warehouse_id", UUID.class),
            row.getString("reference_number"),
            row.getString("purchase_order_number"),
            InboundStatus.valueOf(row.getString("status")),
            row.getString("notes"),
            row.getString("rejection_note"),
            row.getObject("submitted_at", OffsetDateTime.class),
            row.getObject("reviewed_at", OffsetDateTime.class),
            row.getObject("created_by_user_id", UUID.class),
            row.getObject("reviewed_by_user_id", UUID.class),
            row.getObject("created_at", OffsetDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public UUID activeUserId(String email) {
        List<UUID> ids = jdbcTemplate.query("""
                SELECT id FROM users WHERE email = ? AND deleted_at IS NULL AND is_active
                """, (row, index) -> row.getObject("id", UUID.class), email);
        if (ids.isEmpty()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Current user was not found.");
        }
        return ids.get(0);
    }

    public boolean isActiveProduct(UUID productId) {
        Boolean active = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM products
                    WHERE id = ? AND deleted_at IS NULL AND is_active)
                """, Boolean.class, productId);
        return Boolean.TRUE.equals(active);
    }

    public UUID insert(UUID warehouseId, String referenceNumber, String purchaseOrderNumber,
                       String notes, UUID createdByUserId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO inbounds(warehouse_id, reference_number, purchase_order_number,
                    status, notes, created_by_user_id)
                VALUES (?, ?, ?, 'PENDING', ?, ?)
                RETURNING id
                """, UUID.class, warehouseId, referenceNumber, purchaseOrderNumber, notes, createdByUserId);
    }

    public void insertItems(UUID inboundId, List<InboundItemInput> items) {
        for (InboundItemInput item : items) {
            jdbcTemplate.update("""
                    INSERT INTO inbound_items(inbound_id, product_id, quantity, notes)
                    VALUES (?, ?, ?, ?)
                    """, inboundId, item.productId(), item.quantity(), item.notes());
        }
    }

    public void replaceItems(UUID inboundId, List<InboundItemInput> items) {
        jdbcTemplate.update("DELETE FROM inbound_items WHERE inbound_id = ?", inboundId);
        insertItems(inboundId, items);
    }

    public Optional<Header> findHeader(UUID id) {
        List<Header> result = jdbcTemplate.query("""
                SELECT id, warehouse_id, reference_number, purchase_order_number, status, notes,
                    rejection_note, submitted_at, reviewed_at, created_by_user_id, reviewed_by_user_id, created_at
                FROM inbounds WHERE id = ? AND deleted_at IS NULL
                """, HEADER_MAPPER, id);
        return result.stream().findFirst();
    }

    public Header lockHeader(UUID id) {
        List<Header> result = jdbcTemplate.query("""
                SELECT id, warehouse_id, reference_number, purchase_order_number, status, notes,
                    rejection_note, submitted_at, reviewed_at, created_by_user_id, reviewed_by_user_id, created_at
                FROM inbounds WHERE id = ? AND deleted_at IS NULL FOR UPDATE
                """, HEADER_MAPPER, id);
        if (result.isEmpty()) {
            throw notFound();
        }
        return result.get(0);
    }

    public List<InboundItemRow> findItems(UUID inboundId) {
        return jdbcTemplate.query("""
                SELECT ii.id, ii.product_id, p.sku, p.name, ii.quantity, ii.notes
                FROM inbound_items ii JOIN products p ON p.id = ii.product_id
                WHERE ii.inbound_id = ? AND ii.deleted_at IS NULL
                ORDER BY ii.created_at, ii.id
                """, (row, index) -> new InboundItemRow(
                row.getObject("id", UUID.class), row.getObject("product_id", UUID.class),
                row.getString("sku"), row.getString("name"), row.getInt("quantity"), row.getString("notes")), inboundId);
    }

    public void updateHeader(UUID id, String purchaseOrderNumber, String notes) {
        jdbcTemplate.update("""
                UPDATE inbounds SET purchase_order_number = ?, notes = ?, status = 'PENDING',
                    rejection_note = NULL, submitted_at = NULL, reviewed_at = NULL,
                    reviewed_by_user_id = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted_at IS NULL
                """, purchaseOrderNumber, notes, id);
    }

    public int itemCount(UUID id) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM inbound_items WHERE inbound_id = ? AND deleted_at IS NULL
                """, Integer.class, id);
        return count == null ? 0 : count;
    }

    public void submit(UUID id) {
        jdbcTemplate.update("UPDATE inbounds SET submitted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                id);
    }

    public void approve(UUID id, UUID reviewerId) {
        jdbcTemplate.update("""
                UPDATE inbounds SET status = 'APPROVED', reviewed_by_user_id = ?,
                    reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, reviewerId, id);
    }

    public void reject(UUID id, UUID reviewerId, String rejectionNote) {
        jdbcTemplate.update("""
                UPDATE inbounds SET status = 'REJECTED', rejection_note = ?, reviewed_by_user_id = ?,
                    reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, rejectionNote, reviewerId, id);
    }

    public void softDelete(UUID id) {
        jdbcTemplate.update("UPDATE inbound_items SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE inbound_id = ? AND deleted_at IS NULL", id);
        jdbcTemplate.update("UPDATE inbounds SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                id);
    }

    public PageResponse<InboundListItemResponse> list(String email, BasePageRequest request) {
        if (request.getPage() < 1 || request.getSize() < 1 || request.getSize() > 100
                || !SORT_FIELDS.contains(request.getSort())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported inbound sort field.");
        }
        String direction = MasterPage.parseDirection(request.getOrder()).name();
        String sortColumn = switch (request.getSort()) {
            case "reference_number" -> "i.reference_number";
            case "status" -> "i.status";
            default -> "i.created_at";
        };
        String pattern = MasterPage.normalizeSearch(request.getSearch());
        pattern = pattern == null ? null : "%" + pattern + "%";
        String searchClause = pattern == null ? "" : " AND (i.reference_number ILIKE ? "
                + "OR COALESCE(i.purchase_order_number, '') ILIKE ? OR COALESCE(i.notes, '') ILIKE ?)";

        List<Object> args = new ArrayList<>();
        args.add(email);
        if (pattern != null) {
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        Long total = jdbcTemplate.queryForObject("SELECT count(*) " + VISIBLE_INBOUNDS + searchClause,
                Long.class, args.toArray());
        String sql = """
                SELECT i.id, i.warehouse_id, i.reference_number, i.purchase_order_number, i.status,
                    i.notes, i.rejection_note, i.submitted_at, i.reviewed_at,
                    i.created_by_user_id, i.reviewed_by_user_id, i.created_at,
                    (SELECT count(*) FROM inbound_items ii WHERE ii.inbound_id = i.id AND ii.deleted_at IS NULL) item_count
                """ + VISIBLE_INBOUNDS + searchClause + " ORDER BY " + sortColumn + " " + direction
                + ", i.id LIMIT ? OFFSET ?";
        args.add(request.getSize());
        args.add((long) (request.getPage() - 1) * request.getSize());
        List<InboundListItemResponse> content = jdbcTemplate.query(sql, LIST_ITEM_MAPPER, args.toArray());
        long count = total == null ? 0 : total;
        int pages = (int) ((count + request.getSize() - 1) / request.getSize());
        return new PageResponse<>(content, request.getPage(), request.getSize(), count, pages);
    }

    public boolean isActiveLocationInWarehouse(UUID locationId, UUID warehouseId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM warehouse_locations
                    WHERE id = ? AND warehouse_id = ? AND deleted_at IS NULL AND is_active)
                """, Boolean.class, locationId, warehouseId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean hasPutaway(UUID inboundId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM stock_movements
                    WHERE source_entity_type = 'INBOUND' AND source_entity_id = ? AND deleted_at IS NULL)
                """, Boolean.class, inboundId);
        return Boolean.TRUE.equals(exists);
    }

    public void audit(UUID userId, String action, UUID inboundId, String description) {
        jdbcTemplate.update("""
                INSERT INTO audit_trails(user_id, action, entity_type, entity_id, description)
                VALUES (?, ?, 'INBOUND', ?, ?)
                """, userId, action, inboundId, description);
    }

    public InboundResponse response(Header header) {
        List<InboundItemResponse> items = findItems(header.id()).stream()
                .map(item -> new InboundItemResponse(item.id(), item.productId(), item.productSku(),
                        item.productName(), item.quantity(), item.notes()))
                .toList();
        return new InboundResponse(header.id(), header.warehouseId(), header.referenceNumber(),
                header.purchaseOrderNumber(), header.status(), header.notes(), header.rejectionNote(),
                header.submittedAt(), header.reviewedAt(), header.createdByUserId(),
                header.reviewedByUserId(), header.createdAt(), items);
    }

    private static final RowMapper<InboundListItemResponse> LIST_ITEM_MAPPER = (row, index) ->
            new InboundListItemResponse(row.getObject("id", UUID.class),
                    row.getObject("warehouse_id", UUID.class), row.getString("reference_number"),
                    row.getString("purchase_order_number"), InboundStatus.valueOf(row.getString("status")),
                    row.getString("notes"), row.getString("rejection_note"),
                    row.getObject("submitted_at", OffsetDateTime.class), row.getObject("reviewed_at", OffsetDateTime.class),
                    row.getObject("created_by_user_id", UUID.class), row.getObject("reviewed_by_user_id", UUID.class),
                    row.getObject("created_at", OffsetDateTime.class), row.getInt("item_count"));

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "INBOUND_NOT_FOUND", "Inbound was not found.");
    }

    public record Header(UUID id, UUID warehouseId, String referenceNumber, String purchaseOrderNumber,
                         InboundStatus status, String notes, String rejectionNote, OffsetDateTime submittedAt,
                         OffsetDateTime reviewedAt, UUID createdByUserId, UUID reviewedByUserId,
                         OffsetDateTime createdAt) {
    }

    public record InboundItemInput(UUID productId, int quantity, String notes) {
    }

    public record InboundItemRow(UUID id, UUID productId, String productSku, String productName,
                                 int quantity, String notes) {
    }
}
