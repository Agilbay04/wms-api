package com.warehousing.wmsapi.product.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.product.dto.ProductRequest;
import com.warehousing.wmsapi.product.dto.ProductResponse;
import java.util.UUID;

public interface ProductService {
    ProductResponse create(ProductRequest request);
    PageResponse<ProductResponse> list(int page, int size, String sort);
    ProductResponse get(UUID id);
    ProductResponse update(UUID id, ProductRequest request);
    void delete(UUID id);
}
