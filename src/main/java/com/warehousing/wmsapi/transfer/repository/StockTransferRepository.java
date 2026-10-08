package com.warehousing.wmsapi.transfer.repository;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import com.warehousing.wmsapi.transfer.enums.StockTransferStatus;
import com.warehousing.wmsapi.transfer.dto.StockTransferItemResponse;
import com.warehousing.wmsapi.transfer.dto.StockTransferListItemResponse;
import com.warehousing.wmsapi.transfer.dto.StockTransferResponse;
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
public class StockTransferRepository {
    private static final Set<String> SORT_FIELDS = Set.of("created_at", "reference_number", "status");
    private static final String VISIBLE_TRANSFERS = """
            FROM stock_transfers st
            JOIN users u ON u.email = ? AND u.deleted_at IS NULL AND u.is_active
            WHERE st.deleted_at IS NULL AND (EXISTS (
                SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = u.id AND ur.deleted_at IS NULL AND r.deleted_at IS NULL AND r.name = 'SUPERADMIN')
                OR EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id
                AND uw.warehouse_id = st.warehouse_id AND uw.deleted_at IS NULL))
            """;
    private static final RowMapper<Header> HEADER_MAPPER = (row, index) -> new Header(
            row.getObject("id", UUID.class), row.getObject("warehouse_id", UUID.class),
            row.getString("reference_number"), StockTransferStatus.valueOf(row.getString("status")),
            row.getString("notes"), row.getString("rejection_note"),
            row.getObject("submitted_at", OffsetDateTime.class), row.getObject("reviewed_at", OffsetDateTime.class),
            row.getObject("created_by_user_id", UUID.class), row.getObject("reviewed_by_user_id", UUID.class),
            row.getObject("created_at", OffsetDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public StockTransferRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
                INSERT INTO stock_transfers(warehouse_id, reference_number, status, notes, created_by_user_id)
                VALUES (?, ?, 'PENDING', ?, ?) RETURNING id
                """, UUID.class, warehouseId, reference, notes, creatorId);
    }

    public void insertItems(UUID transferId, List<ItemInput> items) {
        for (ItemInput item : items) {
            jdbcTemplate.update("""
                    INSERT INTO stock_transfer_items(stock_transfer_id, product_id, source_warehouse_location_id,
                        destination_warehouse_location_id, quantity, notes)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, transferId, item.productId(), item.sourceLocationId(), item.destinationLocationId(),
                    item.quantity(), item.notes());
        }
    }

    public void replaceItems(UUID transferId, List<ItemInput> items) {
        jdbcTemplate.update("DELETE FROM stock_transfer_items WHERE stock_transfer_id = ?", transferId);
        insertItems(transferId, items);
    }

    public Optional<Header> findHeader(UUID id) {
        List<Header> rows = jdbcTemplate.query("""
                SELECT id, warehouse_id, reference_number, status, notes, rejection_note, submitted_at, reviewed_at,
                    created_by_user_id, reviewed_by_user_id, created_at
                FROM stock_transfers WHERE id = ? AND deleted_at IS NULL
                """, HEADER_MAPPER, id);
        return rows.stream().findFirst();
    }

    public Header lockHeader(UUID id) {
        List<Header> rows = jdbcTemplate.query("""
                SELECT id, warehouse_id, reference_number, status, notes, rejection_note, submitted_at, reviewed_at,
                    created_by_user_id, reviewed_by_user_id, created_at
                FROM stock_transfers WHERE id = ? AND deleted_at IS NULL FOR UPDATE
                """, HEADER_MAPPER, id);
        if (rows.isEmpty()) {
            throw notFound();
        }
        return rows.get(0);
    }

    public List<ItemRow> findItems(UUID transferId) {
        return jdbcTemplate.query("""
                SELECT sti.id, sti.product_id, p.sku, p.name, sti.source_warehouse_location_id,
                    source.code source_code, sti.destination_warehouse_location_id, destination.code destination_code,
                    sti.quantity, sti.notes
                FROM stock_transfer_items sti
                JOIN products p ON p.id = sti.product_id
                JOIN warehouse_locations source ON source.id = sti.source_warehouse_location_id
                JOIN warehouse_locations destination ON destination.id = sti.destination_warehouse_location_id
                WHERE sti.stock_transfer_id = ? AND sti.deleted_at IS NULL
                ORDER BY sti.created_at, sti.id
                """, (row, index) -> new ItemRow(row.getObject("id", UUID.class),
                row.getObject("product_id", UUID.class), row.getString("sku"), row.getString("name"),
                row.getObject("source_warehouse_location_id", UUID.class), row.getString("source_code"),
                row.getObject("destination_warehouse_location_id", UUID.class), row.getString("destination_code"),
                row.getInt("quantity"), row.getString("notes")), transferId);
    }

    public int itemCount(UUID transferId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM stock_transfer_items WHERE stock_transfer_id = ? AND deleted_at IS NULL
                """, Integer.class, transferId);
        return count == null ? 0 : count;
    }

    public void submit(UUID id) {
        jdbcTemplate.update("UPDATE stock_transfers SET submitted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                id);
    }

    public void updateHeader(UUID id, String notes) {
        jdbcTemplate.update("""
                UPDATE stock_transfers SET notes = ?, status = 'PENDING', rejection_note = NULL,
                    submitted_at = NULL, reviewed_at = NULL, reviewed_by_user_id = NULL,
                    updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, notes, id);
    }

    public void approve(UUID id, UUID reviewerId) {
        jdbcTemplate.update("""
                UPDATE stock_transfers SET status = 'APPROVED', reviewed_by_user_id = ?,
                    reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, reviewerId, id);
    }

    public void reject(UUID id, UUID reviewerId, String note) {
        jdbcTemplate.update("""
                UPDATE stock_transfers SET status = 'REJECTED', rejection_note = ?, reviewed_by_user_id = ?,
                    reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, note, reviewerId, id);
    }

    public void softDelete(UUID id) {
        jdbcTemplate.update("""
                UPDATE stock_transfer_items SET deleted_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP WHERE stock_transfer_id = ? AND deleted_at IS NULL""", id);
        jdbcTemplate.update("""
                UPDATE stock_transfers SET deleted_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP WHERE id = ?""", id);
    }

    public boolean hasApproval(UUID id) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM stock_movements WHERE source_entity_type = 'STOCK_TRANSFER'
                    AND source_entity_id = ? AND deleted_at IS NULL)
                """, Boolean.class, id);
        return Boolean.TRUE.equals(exists);
    }

    public void audit(UUID userId, String action, UUID transferId, String description) {
        jdbcTemplate.update("""
                INSERT INTO audit_trails(user_id, action, entity_type, entity_id, description)
                VALUES (?, ?, 'STOCK_TRANSFER', ?, ?)
                """, userId, action, transferId, description);
    }

    public PageResponse<StockTransferListItemResponse> list(String email, BasePageRequest request) {
        if (request.getPage() < 1 || request.getSize() < 1 || request.getSize() > 100
                || !SORT_FIELDS.contains(request.getSort())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported stock transfer sort field.");
        }
        String sort = switch (request.getSort()) {
            case "reference_number" -> "st.reference_number";
            case "status" -> "st.status";
            default -> "st.created_at";
        };
        String direction = MasterPage.parseDirection(request.getOrder()).name();
        String search = MasterPage.normalizeSearch(request.getSearch());
        String pattern = search == null ? null : "%" + search + "%";
        String filter = pattern == null ? "" : " AND (st.reference_number ILIKE ? OR COALESCE(st.notes, '') ILIKE ?)";
        List<Object> args = new ArrayList<>();
        args.add(email);
        if (pattern != null) { args.add(pattern); args.add(pattern); }
        Long count = jdbcTemplate.queryForObject("SELECT count(*) " + VISIBLE_TRANSFERS + filter, Long.class,
                args.toArray());
        args.add(request.getSize());
        args.add((long) (request.getPage() - 1) * request.getSize());
        String sql = """
                SELECT st.id, st.warehouse_id, st.reference_number, st.status, st.notes, st.rejection_note,
                    st.submitted_at, st.reviewed_at, st.created_by_user_id, st.reviewed_by_user_id, st.created_at,
                    (SELECT count(*) FROM stock_transfer_items sti WHERE sti.stock_transfer_id = st.id
                        AND sti.deleted_at IS NULL) item_count
                """ + VISIBLE_TRANSFERS + filter + " ORDER BY " + sort + " " + direction + ", st.id LIMIT ? OFFSET ?";
        List<StockTransferListItemResponse> content = jdbcTemplate.query(sql, (row, index) ->
                new StockTransferListItemResponse(row.getObject("id", UUID.class),
                        row.getObject("warehouse_id", UUID.class), row.getString("reference_number"),
                        StockTransferStatus.valueOf(row.getString("status")), row.getString("notes"),
                        row.getString("rejection_note"), row.getObject("submitted_at", OffsetDateTime.class),
                        row.getObject("reviewed_at", OffsetDateTime.class),
                        row.getObject("created_by_user_id", UUID.class),
                        row.getObject("reviewed_by_user_id", UUID.class),
                        row.getObject("created_at", OffsetDateTime.class), row.getInt("item_count")), args.toArray());
        long total = count == null ? 0 : count;
        int pages = (int) ((total + request.getSize() - 1) / request.getSize());
        return new PageResponse<>(content, request.getPage(), request.getSize(), total, pages);
    }

    public StockTransferResponse response(Header header) {
        List<StockTransferItemResponse> items = findItems(header.id()).stream()
                .map(item -> new StockTransferItemResponse(item.id(), item.productId(), item.productSku(),
                        item.productName(), item.sourceLocationId(), item.sourceLocationCode(),
                        item.destinationLocationId(), item.destinationLocationCode(), item.quantity(), item.notes()))
                .toList();
        return new StockTransferResponse(header.id(), header.warehouseId(), header.referenceNumber(), header.status(),
                header.notes(), header.rejectionNote(), header.submittedAt(), header.reviewedAt(),
                header.createdByUserId(), header.reviewedByUserId(), header.createdAt(), items);
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "STOCK_TRANSFER_NOT_FOUND", "Stock transfer was not found.");
    }

    public record Header(UUID id, UUID warehouseId, String referenceNumber, StockTransferStatus status,
            String notes, String rejectionNote, OffsetDateTime submittedAt, OffsetDateTime reviewedAt,
            UUID createdByUserId, UUID reviewedByUserId, OffsetDateTime createdAt) { }
    public record ItemInput(UUID productId, UUID sourceLocationId, UUID destinationLocationId, int quantity,
            String notes) { }
    public record ItemRow(UUID id, UUID productId, String productSku, String productName, UUID sourceLocationId,
            String sourceLocationCode, UUID destinationLocationId, String destinationLocationCode,
            int quantity, String notes) { }
}
