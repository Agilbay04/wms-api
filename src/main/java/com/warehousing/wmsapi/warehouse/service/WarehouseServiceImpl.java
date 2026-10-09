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
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {
    private static final Set<String> SORT_FIELDS = Set.of("code", "name", "createdAt", "created_at");
    private final WarehouseRepository repository;
    private final com.warehousing.wmsapi.warehouse.repository.WarehouseQueryRepository queryRepository;
    private final WarehouseAccessService accessService;

    @Transactional
    @CacheEvict(cacheNames = {"warehouses", "dashboards", "stock-summaries"}, allEntries = true)
    @Override
    public WarehouseResponse create(WarehouseRequest request) {
        ensureCodeAvailable(request.code());
        return WarehouseResponse.from(repository.save(new WarehouseEntity(
                request.code(), request.name(), request.address(), request.active())));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "warehouses",
            key = "#p0.name + ':' + #p1.page + ':' + #p1.size + ':' + #p1.sort + ':' + #p1.order + ':' + #p1.search")
    @Override
    public PageResponse<WarehouseResponse> list(Authentication authentication, BasePageRequest request) {
        String search = MasterPage.normalizeSearch(request.getSearch());
        String searchPattern = search == null ? null : "%" + search + "%";
        if (request.getPage() < 1 || request.getSize() < 1 || request.getSize() > 100
                || !SORT_FIELDS.contains(request.getSort())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported sort field.");
        }
        String sortDirection = MasterPage.parseDirection(request.getOrder()).name();
        String searchArg = searchPattern;
        String sortKey = switch (request.getSort()) {
            case "name" -> "name";
            case "createdAt", "created_at" -> "created_at";
            default -> "code";
        };
        long totalElements = queryRepository.countVisible(authentication.getName(), searchArg);
        var content = queryRepository.findVisible(authentication.getName(), searchArg, sortKey,
                sortDirection, request.getSize(), (long) (request.getPage() - 1) * request.getSize());
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
    @CacheEvict(cacheNames = {"warehouses", "dashboards", "stock-summaries"}, allEntries = true)
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
    @CacheEvict(cacheNames = {"warehouses", "dashboards", "stock-summaries"}, allEntries = true)
    @Override
    public void delete(Authentication authentication, UUID id) {
        requireAccess(authentication, id);
        WarehouseEntity warehouse = find(id);
        if (queryRepository.isReferenced(id)) {
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
