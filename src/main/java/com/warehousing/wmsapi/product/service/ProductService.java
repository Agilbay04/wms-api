package com.warehousing.wmsapi.product.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.product.dto.ProductRequest;
import com.warehousing.wmsapi.product.dto.ProductResponse;
import java.util.UUID;

public interface ProductService {
    ProductResponse create(ProductRequest request);
    PageResponse<ProductResponse> list(BasePageRequest request);
    default PageResponse<ProductResponse> list(int page, int size, String sort, String order, String search) {
        return list(BasePageRequest.of(page, size, sort, order, search));
    }
    default PageResponse<ProductResponse> list(int page, int size, String sort, String order) {
        return list(page, size, sort, order, null);
    }
    default PageResponse<ProductResponse> list(int page, int size, String sort) {
        return list(page, size, sort, "desc");
    }
    ProductResponse get(UUID id);
    ProductResponse update(UUID id, ProductRequest request);
    void delete(UUID id);
}
