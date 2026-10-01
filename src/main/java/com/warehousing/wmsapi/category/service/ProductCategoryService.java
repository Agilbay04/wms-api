package com.warehousing.wmsapi.category.service;

import com.warehousing.wmsapi.category.dto.CategoryRequest;
import com.warehousing.wmsapi.category.dto.CategoryResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import java.util.UUID;

public interface ProductCategoryService {
    CategoryResponse create(CategoryRequest request);
    PageResponse<CategoryResponse> list(int page, int size, String sort);
    CategoryResponse get(UUID id);
    CategoryResponse update(UUID id, CategoryRequest request);
    void delete(UUID id);
}
