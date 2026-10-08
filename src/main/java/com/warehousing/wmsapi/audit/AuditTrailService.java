package com.warehousing.wmsapi.audit;

import com.warehousing.wmsapi.audit.dto.AuditTrailRequest;
import com.warehousing.wmsapi.audit.dto.AuditTrailResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTrailService {
    private static final Set<String> ENTITIES = Set.of("INBOUND", "OUTBOUND", "STOCK_TRANSFER", "STOCK_ADJUSTMENT");
    private static final Set<String> ACTIONS = Set.of("CREATE", "UPDATE", "DELETE", "REQUEST_APPROVAL", "APPROVE", "REJECT", "EXPORT");
    private static final Map<String, String> SORTS = Map.of("occurred_at", "a.occurred_at",
            "created_at", "a.created_at", "action", "a.action", "entity_type", "a.entity_type");
    private static final String VISIBLE_AUDIT = """
            FROM audit_trails a
            JOIN users actor ON actor.id = a.user_id AND actor.deleted_at IS NULL
            JOIN (SELECT 'INBOUND' entity_type, id entity_id, warehouse_id FROM inbounds
                  UNION ALL SELECT 'OUTBOUND', id, warehouse_id FROM outbounds
                  UNION ALL SELECT 'STOCK_TRANSFER', id, warehouse_id FROM stock_transfers
                  UNION ALL SELECT 'STOCK_ADJUSTMENT', id, warehouse_id FROM stock_adjustments
            ) entity ON entity.entity_type = a.entity_type AND entity.entity_id = a.entity_id
            JOIN users viewer ON viewer.email = ? AND viewer.deleted_at IS NULL AND viewer.is_active
            WHERE a.deleted_at IS NULL AND (EXISTS (
                SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = viewer.id AND ur.deleted_at IS NULL AND r.deleted_at IS NULL AND r.name = 'SUPERADMIN')
                OR EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = viewer.id
                    AND uw.warehouse_id = entity.warehouse_id AND uw.deleted_at IS NULL))
            """;

    private final JdbcTemplate jdbcTemplate;

    public AuditTrailService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditTrailResponse> list(Authentication authentication, AuditTrailRequest request) {
        String sort = SORTS.get(request.getSort());
        if (request.getPage() == null || request.getSize() == null || request.getPage() < 1
                || request.getSize() < 1 || request.getSize() > 100 || sort == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported audit sort field.");
        }
        validateDates(request.getFromDate(), request.getToDate());
        String entityType = normalized(request.getEntityType());
        String action = normalized(request.getAction());
        if (entityType != null && !ENTITIES.contains(entityType)) invalidFilter("entity_type");
        if (action != null && !ACTIONS.contains(action)) invalidFilter("action");
        List<Object> args = new ArrayList<>();
        args.add(authentication.getName());
        String filters = "";
        if (entityType != null) { filters += " AND a.entity_type = ?"; args.add(entityType); }
        if (action != null) { filters += " AND a.action = ?"; args.add(action); }
        if (request.getUserId() != null) { filters += " AND a.user_id = ?"; args.add(request.getUserId()); }
        if (request.getFromDate() != null) {
            filters += " AND a.occurred_at >= ?";
            args.add(request.getFromDate().atStartOfDay().atOffset(ZoneOffset.UTC));
        }
        if (request.getToDate() != null) {
            filters += " AND a.occurred_at < ?";
            args.add(request.getToDate().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
        }
        String pattern = normalized(request.getSearch());
        if (pattern != null) {
            filters += " AND (a.description ILIKE ? OR actor.email ILIKE ? OR actor.name ILIKE ? OR a.entity_id::text ILIKE ?)";
            String wildcard = "%" + pattern + "%";
            args.add(wildcard); args.add(wildcard); args.add(wildcard); args.add(wildcard);
        }
        long total = count(args, filters);
        args.add(request.getSize()); args.add((long) (request.getPage() - 1) * request.getSize());
        List<AuditTrailResponse> content = jdbcTemplate.query("""
                SELECT a.id, actor.id user_id, actor.name user_name, actor.email user_email,
                    a.action, a.entity_type, a.entity_id, a.description, a.occurred_at
                """ + VISIBLE_AUDIT + filters + " ORDER BY " + sort + " " + MasterPage.parseDirection(request.getOrder())
                + ", a.id LIMIT ? OFFSET ?", (row, index) -> new AuditTrailResponse(
                        row.getObject("id", java.util.UUID.class), row.getObject("user_id", java.util.UUID.class),
                        row.getString("user_name"), row.getString("user_email"), row.getString("action"),
                        row.getString("entity_type"), row.getObject("entity_id", java.util.UUID.class),
                        row.getString("description"), row.getObject("occurred_at", OffsetDateTime.class)), args.toArray());
        int pages = (int) ((total + request.getSize() - 1) / request.getSize());
        return new PageResponse<>(content, request.getPage(), request.getSize(), total, pages);
    }

    private long count(List<Object> args, String filters) {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) " + VISIBLE_AUDIT + filters,
                Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "from_date must be earlier than or equal to to_date.");
        }
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private void invalidFilter(String field) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AUDIT_FILTER", "Unsupported " + field + " filter.");
    }
}
