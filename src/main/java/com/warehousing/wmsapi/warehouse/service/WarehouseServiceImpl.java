package com.warehousing.wmsapi.warehouse.service;

import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;
import com.warehousing.wmsapi.warehouse.repository.WarehouseRepository;
import com.warehousing.wmsapi.warehouse.dto.WarehouseRequest;
import com.warehousing.wmsapi.warehouse.dto.WarehouseResponse;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.security.WarehouseAccessService;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseServiceImpl implements WarehouseService {
    private static final Set<String> SORT_FIELDS = Set.of("code", "name", "createdAt");
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
    public PageResponse<WarehouseResponse> list(Authentication authentication, int page, int size, String sort) {
        if (page < 1 || size < 1 || size > 100 || !SORT_FIELDS.contains(sort)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported sort field.");
        }
        String sortColumn = switch (sort) {
            case "name" -> "w.name";
            case "createdAt" -> "w.created_at";
            default -> "w.code";
        };
        Long total = jdbcTemplate.queryForObject("SELECT count(*) " + VISIBLE_WAREHOUSES,
                Long.class, authentication.getName());
        var content = jdbcTemplate.query("""
                        SELECT w.id, w.code, w.name, w.address, w.is_active
                        """ + VISIBLE_WAREHOUSES + " ORDER BY " + sortColumn + ", w.id LIMIT ? OFFSET ?",
                (row, index) -> new WarehouseResponse(
                        row.getObject("id", UUID.class), row.getString("code"), row.getString("name"),
                        row.getString("address"), row.getBoolean("is_active")),
                authentication.getName(), size, (long) (page - 1) * size);
        long totalElements = total == null ? 0 : total;
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
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
