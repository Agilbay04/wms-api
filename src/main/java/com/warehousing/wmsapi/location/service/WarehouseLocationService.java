package com.warehousing.wmsapi.location.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.location.dto.LocationRequest;
import com.warehousing.wmsapi.location.dto.LocationResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface WarehouseLocationService {
    LocationResponse create(Authentication authentication, UUID warehouseId, LocationRequest request);
    PageResponse<LocationResponse> list(Authentication authentication, UUID warehouseId, int page, int size, String sort);
    LocationResponse get(Authentication authentication, UUID warehouseId, UUID id);
    LocationResponse update(Authentication authentication, UUID warehouseId, UUID id, LocationRequest request);
    void delete(Authentication authentication, UUID warehouseId, UUID id);
}
