package com.warehousing.wmsapi.category.service;

import com.warehousing.wmsapi.category.entity.ProductCategoryEntity;
import com.warehousing.wmsapi.category.repository.ProductCategoryRepository;
import com.warehousing.wmsapi.category.dto.CategoryRequest;
import com.warehousing.wmsapi.category.dto.CategoryResponse;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import com.warehousing.wmsapi.product.repository.ProductRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {
    private static final Set<String> SORT_FIELDS = Set.of("code", "name", "createdAt");
    private final ProductCategoryRepository repository;
    private final ProductRepository productRepository;

    public ProductCategoryServiceImpl(ProductCategoryRepository repository, ProductRepository productRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Override
    public CategoryResponse create(CategoryRequest request) {
        ensureCodeAvailable(request.code());
        return CategoryResponse.from(repository.save(new ProductCategoryEntity(
                request.code(), request.name(), request.description(), request.active())));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories",
            key = "#p0.page + ':' + #p0.size + ':' + #p0.sort + ':' + #p0.order + ':' + #p0.search")
    @Override
    public PageResponse<CategoryResponse> list(BasePageRequest request) {
        String search = MasterPage.normalizeSearch(request.getSearch());
        var pageable = MasterPage.of(
                request.getPage(), request.getSize(), request.getSort(), request.getOrder(), SORT_FIELDS);
        var result = search == null
                ? repository.findAllByDeletedAtIsNull(pageable)
                : repository.searchActive(search, pageable);
        return new PageResponse<>(result.map(CategoryResponse::from).getContent(), request.getPage(), request.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    @Override
    public CategoryResponse get(UUID id) { return CategoryResponse.from(find(id)); }

    @Transactional
    @CacheEvict(cacheNames = {"categories", "products"}, allEntries = true)
    @Override
    public CategoryResponse update(UUID id, CategoryRequest request) {
        ProductCategoryEntity category = find(id);
        if (!request.active() && category.isActive()
                && productRepository.existsByCategory_IdAndDeletedAtIsNull(id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CATEGORY_IN_USE",
                    "Deactivate or move the products before deactivating this category.");
        }
        if (!category.getCode().equals(request.code())) {
            ensureCodeAvailable(request.code());
        }
        category.update(request.code(), request.name(), request.description(), request.active());
        return CategoryResponse.from(category);
    }

    @Transactional
    @CacheEvict(cacheNames = {"categories", "products"}, allEntries = true)
    @Override
    public void delete(UUID id) {
        ProductCategoryEntity category = find(id);
        if (productRepository.existsByCategory_IdAndDeletedAtIsNull(id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CATEGORY_IN_USE",
                    "Product category is still used by products.");
        }
        category.markDeleted();
    }

    private ProductCategoryEntity find(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
                        "Product category was not found."));
    }

    private void ensureCodeAvailable(String code) {
        if (repository.existsByCode(code)) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_CATEGORY_CODE",
                    "Category code already exists.");
        }
    }
}
