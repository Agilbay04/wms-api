package com.warehousing.wmsapi.category.service;

import com.warehousing.wmsapi.category.dto.CategoryRequest;
import com.warehousing.wmsapi.category.dto.CategoryResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import java.util.UUID;

public interface ProductCategoryService {
    CategoryResponse create(CategoryRequest request);
    PageResponse<CategoryResponse> list(BasePageRequest request);
    default PageResponse<CategoryResponse> list(int page, int size, String sort, String order, String search) {
        return list(BasePageRequest.of(page, size, sort, order, search));
    }
    default PageResponse<CategoryResponse> list(int page, int size, String sort, String order) {
        return list(page, size, sort, order, null);
    }
    default PageResponse<CategoryResponse> list(int page, int size, String sort) {
        return list(page, size, sort, "desc");
    }
    CategoryResponse get(UUID id);
    CategoryResponse update(UUID id, CategoryRequest request);
    void delete(UUID id);
}
