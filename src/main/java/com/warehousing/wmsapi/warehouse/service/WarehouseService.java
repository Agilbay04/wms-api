package com.warehousing.wmsapi.warehouse.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.warehouse.dto.WarehouseRequest;
import com.warehousing.wmsapi.warehouse.dto.WarehouseResponse;
import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface WarehouseService {
    WarehouseResponse create(WarehouseRequest request);
    PageResponse<WarehouseResponse> list(Authentication authentication, int page, int size, String sort);
    WarehouseResponse get(Authentication authentication, UUID id);
    WarehouseResponse update(Authentication authentication, UUID id, WarehouseRequest request);
    void delete(Authentication authentication, UUID id);
    WarehouseEntity find(UUID id);
    void requireAccess(Authentication authentication, UUID id);
}
