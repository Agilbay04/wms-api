package com.warehousing.wmsapi.warehouse.service;

import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;
import com.warehousing.wmsapi.warehouse.repository.WarehouseRepository;
import com.warehousing.wmsapi.warehouse.dto.WarehouseRequest;
import com.warehousing.wmsapi.warehouse.dto.WarehouseResponse;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import com.warehousing.wmsapi.common.security.WarehouseAccessService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseServiceImpl implements WarehouseService {
    private static final Set<String> SORT_FIELDS = Set.of("code", "name", "createdAt", "created_at");
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
    private final WarehouseRepository repository;
    private final WarehouseAccessService accessService;
    private final JdbcTemplate jdbcTemplate;

    public WarehouseServiceImpl(WarehouseRepository repository, WarehouseAccessService accessService,
                            JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.accessService = accessService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @Override
    public WarehouseResponse create(WarehouseRequest request) {
        ensureCodeAvailable(request.code());
        return WarehouseResponse.from(repository.save(new WarehouseEntity(
                request.code(), request.name(), request.address(), request.active())));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<WarehouseResponse> list(Authentication authentication, BasePageRequest request) {
        String search = MasterPage.normalizeSearch(request.getSearch());
        String searchPattern = search == null ? null : "%" + search + "%";
        if (request.getPage() < 1 || request.getSize() < 1 || request.getSize() > 100
                || !SORT_FIELDS.contains(request.getSort())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported sort field.");
        }
        String sortColumn = switch (request.getSort()) {
            case "name" -> "w.name";
            case "createdAt", "created_at" -> "w.created_at";
            default -> "w.code";
        };
        String sortDirection = MasterPage.parseDirection(request.getOrder()).name();
        String searchFilter = searchPattern == null ? ""
                : " AND (w.code ILIKE ? OR w.name ILIKE ? OR COALESCE(w.address, '') ILIKE ?)";
        List<Object> queryArguments = new ArrayList<>();
        queryArguments.add(authentication.getName());
        if (searchPattern != null) {
            queryArguments.add(searchPattern);
            queryArguments.add(searchPattern);
            queryArguments.add(searchPattern);
        }
        Long total = jdbcTemplate.queryForObject("SELECT count(*) " + VISIBLE_WAREHOUSES + searchFilter,
                Long.class, queryArguments.toArray());
        String contentSql = """
                        SELECT w.id, w.code, w.name, w.address, w.is_active, w.created_at
                        """ + VISIBLE_WAREHOUSES + searchFilter + " ORDER BY " + sortColumn + " " + sortDirection
                        + ", w.id LIMIT ? OFFSET ?";
        queryArguments.add(request.getSize());
        queryArguments.add((long) (request.getPage() - 1) * request.getSize());
        var content = jdbcTemplate.query(contentSql,
                (row, index) -> new WarehouseResponse(
                        row.getObject("id", UUID.class), row.getString("code"), row.getString("name"),
                        row.getString("address"), row.getBoolean("is_active"),
                        row.getObject("created_at", OffsetDateTime.class)),
                queryArguments.toArray());
        long totalElements = total == null ? 0 : total;
        int totalPages = (int) ((totalElements + request.getSize() - 1) / request.getSize());
        return new PageResponse<>(content, request.getPage(), request.getSize(), totalElements, totalPages);
    }

    @Transactional(readOnly = true)
    @Override
    public WarehouseResponse get(Authentication authentication, UUID id) {
        requireAccess(authentication, id);
        return WarehouseResponse.from(find(id));
    }

    @Transactional
    @Override
    public WarehouseResponse update(Authentication authentication, UUID id, WarehouseRequest request) {
        requireAccess(authentication, id);
        WarehouseEntity warehouse = find(id);
        if (!warehouse.getCode().equals(request.code())) {
            ensureCodeAvailable(request.code());
        }
        warehouse.update(request.code(), request.name(), request.address(), request.active());
        return WarehouseResponse.from(warehouse);
    }

    @Transactional
    @Override
    public void delete(Authentication authentication, UUID id) {
        requireAccess(authentication, id);
        WarehouseEntity warehouse = find(id);
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
        if (Boolean.TRUE.equals(referenced)) {
            throw new BusinessException(HttpStatus.CONFLICT, "WAREHOUSE_IN_USE",
                    "Warehouse is still used by locations, users, or transactions.");
        }
        warehouse.markDeleted();
    }

    @Override
    public WarehouseEntity find(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WAREHOUSE_NOT_FOUND",
                        "Warehouse was not found."));
    }

    @Override
    public void requireAccess(Authentication authentication, UUID id) {
        if (!accessService.canAccessWarehouse(authentication, id)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "WAREHOUSE_FORBIDDEN",
                    "You do not have access to this warehouse.");
        }
    }

    private void ensureCodeAvailable(String code) {
        if (repository.existsByCode(code)) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_WAREHOUSE_CODE",
                    "Warehouse code already exists.");
        }
    }
}
