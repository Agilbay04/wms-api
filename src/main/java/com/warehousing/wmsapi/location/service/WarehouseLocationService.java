package com.warehousing.wmsapi.location.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.location.dto.LocationRequest;
import com.warehousing.wmsapi.location.dto.LocationResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface WarehouseLocationService {
    LocationResponse create(Authentication authentication, UUID warehouseId, LocationRequest request);
    PageResponse<LocationResponse> list(Authentication authentication, UUID warehouseId, BasePageRequest request);
    default PageResponse<LocationResponse> list(Authentication authentication, UUID warehouseId,
                                                int page, int size, String sort, String order, String search) {
        return list(authentication, warehouseId, BasePageRequest.of(page, size, sort, order, search));
    }
    default PageResponse<LocationResponse> list(Authentication authentication, UUID warehouseId,
                                                int page, int size, String sort, String order) {
        return list(authentication, warehouseId, page, size, sort, order, null);
    }
    default PageResponse<LocationResponse> list(Authentication authentication, UUID warehouseId,
                                                int page, int size, String sort) {
        return list(authentication, warehouseId, page, size, sort, "desc");
    }
    LocationResponse get(Authentication authentication, UUID warehouseId, UUID id);
    LocationResponse update(Authentication authentication, UUID warehouseId, UUID id, LocationRequest request);
    void delete(Authentication authentication, UUID warehouseId, UUID id);
}
