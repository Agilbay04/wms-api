package com.warehousing.wmsapi.location.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import com.warehousing.wmsapi.location.dto.LocationResponse;
import com.warehousing.wmsapi.location.repository.WarehouseLocationRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WarehouseLocationListCache {
    private static final Set<String> SORT_FIELDS = Set.of("code", "name", "createdAt");
    private final WarehouseLocationRepository repository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "locations",
            key = "#p0 + ':' + #p1.page + ':' + #p1.size + ':' + #p1.sort + ':' + #p1.order + ':' + #p1.search")
    public PageResponse<LocationResponse> list(java.util.UUID warehouseId, BasePageRequest request) {
        String search = MasterPage.normalizeSearch(request.getSearch());
        var pageable = MasterPage.of(
                request.getPage(), request.getSize(), request.getSort(), request.getOrder(), SORT_FIELDS);
        var result = search == null
                ? repository.findAllByWarehouse_IdAndDeletedAtIsNull(warehouseId, pageable)
                : repository.searchActiveByWarehouse(warehouseId, search, pageable);
        return new PageResponse<>(result.map(LocationResponse::from).getContent(), request.getPage(), request.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
