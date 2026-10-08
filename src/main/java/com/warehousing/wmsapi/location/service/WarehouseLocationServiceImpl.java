package com.warehousing.wmsapi.location.service;

import com.warehousing.wmsapi.location.entity.WarehouseLocationEntity;
import com.warehousing.wmsapi.location.repository.WarehouseLocationRepository;
import com.warehousing.wmsapi.location.dto.LocationRequest;
import com.warehousing.wmsapi.location.dto.LocationResponse;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;
import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseLocationServiceImpl implements WarehouseLocationService {
    private final WarehouseLocationRepository repository;
    private final WarehouseService warehouseService;
    private final WarehouseLocationListCache listCache;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public WarehouseLocationServiceImpl(WarehouseLocationRepository repository, WarehouseService warehouseService,
                                    WarehouseLocationListCache listCache, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.warehouseService = warehouseService;
        this.listCache = listCache;
        this.jdbcTemplate = jdbcTemplate;
    }

    public WarehouseLocationServiceImpl(WarehouseLocationRepository repository, WarehouseService warehouseService,
                                        JdbcTemplate jdbcTemplate) {
        this(repository, warehouseService, new WarehouseLocationListCache(repository), jdbcTemplate);
    }

    @Transactional
    @CacheEvict(cacheNames = "locations", allEntries = true)
    @Override
    public LocationResponse create(Authentication authentication, UUID warehouseId, LocationRequest request) {
        WarehouseEntity warehouse = accessibleActiveWarehouse(authentication, warehouseId);
        ensureCodeAvailable(warehouseId, request.code());
        return LocationResponse.from(repository.save(new WarehouseLocationEntity(warehouse,
                request.code(), request.name(), request.description(), request.active())));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<LocationResponse> list(Authentication authentication, UUID warehouseId,
                                               BasePageRequest request) {
        warehouseService.requireAccess(authentication, warehouseId);
        warehouseService.find(warehouseId);
        return listCache.list(warehouseId, request);
    }

    @Transactional(readOnly = true)
    @Override
    public LocationResponse get(Authentication authentication, UUID warehouseId, UUID id) {
        warehouseService.requireAccess(authentication, warehouseId);
        return LocationResponse.from(find(warehouseId, id));
    }

    @Transactional
    @CacheEvict(cacheNames = "locations", allEntries = true)
    @Override
    public LocationResponse update(Authentication authentication, UUID warehouseId, UUID id, LocationRequest request) {
        accessibleActiveWarehouse(authentication, warehouseId);
        WarehouseLocationEntity location = find(warehouseId, id);
        if (!location.getCode().equals(request.code())) {
            ensureCodeAvailable(warehouseId, request.code());
        }
        location.update(request.code(), request.name(), request.description(), request.active());
        return LocationResponse.from(location);
    }

    @Transactional
    @CacheEvict(cacheNames = "locations", allEntries = true)
    @Override
    public void delete(Authentication authentication, UUID warehouseId, UUID id) {
        warehouseService.requireAccess(authentication, warehouseId);
        WarehouseLocationEntity location = find(warehouseId, id);
        Boolean referenced = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM warehouse_location_items WHERE warehouse_location_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM stock_movements WHERE warehouse_location_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM stock_transfer_items WHERE source_warehouse_location_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM stock_transfer_items WHERE destination_warehouse_location_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM stock_adjustment_items WHERE warehouse_location_id = ? AND deleted_at IS NULL
                    UNION ALL SELECT 1 FROM outbound_items WHERE warehouse_location_id = ? AND deleted_at IS NULL
                )
                """, Boolean.class, id, id, id, id, id, id);
        if (Boolean.TRUE.equals(referenced)) {
            throw new BusinessException(HttpStatus.CONFLICT, "LOCATION_IN_USE",
                    "Warehouse location is still used by stock or transactions.");
        }
        location.markDeleted();
    }

    private WarehouseLocationEntity find(UUID warehouseId, UUID id) {
        return repository.findByIdAndWarehouse_IdAndDeletedAtIsNull(id, warehouseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "LOCATION_NOT_FOUND",
                        "Warehouse location was not found."));
    }

    private WarehouseEntity accessibleActiveWarehouse(Authentication authentication, UUID warehouseId) {
        warehouseService.requireAccess(authentication, warehouseId);
        WarehouseEntity warehouse = warehouseService.find(warehouseId);
        if (!warehouse.isActive()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INACTIVE_WAREHOUSE",
                    "Choose an active warehouse.");
        }
        return warehouse;
    }

    private void ensureCodeAvailable(UUID warehouseId, String code) {
        if (repository.existsByWarehouse_IdAndCode(warehouseId, code)) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_LOCATION_CODE",
                    "Location code already exists in this warehouse.");
        }
    }
}
