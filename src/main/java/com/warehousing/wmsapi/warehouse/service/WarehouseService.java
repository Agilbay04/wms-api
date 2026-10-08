package com.warehousing.wmsapi.warehouse.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.warehouse.dto.WarehouseRequest;
import com.warehousing.wmsapi.warehouse.dto.WarehouseResponse;
import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface WarehouseService {
    WarehouseResponse create(WarehouseRequest request);
    PageResponse<WarehouseResponse> list(Authentication authentication, BasePageRequest request);
    default PageResponse<WarehouseResponse> list(Authentication authentication, int page, int size,
                                                   String sort, String order, String search) {
        return list(authentication, BasePageRequest.of(page, size, sort, order, search));
    }
    default PageResponse<WarehouseResponse> list(Authentication authentication, int page, int size,
                                                   String sort, String order) {
        return list(authentication, page, size, sort, order, null);
    }
    default PageResponse<WarehouseResponse> list(Authentication authentication, int page, int size, String sort) {
        return list(authentication, page, size, sort, "desc");
    }
    WarehouseResponse get(Authentication authentication, UUID id);
    WarehouseResponse update(Authentication authentication, UUID id, WarehouseRequest request);
    void delete(Authentication authentication, UUID id);
    WarehouseEntity find(UUID id);
    void requireAccess(Authentication authentication, UUID id);
}
