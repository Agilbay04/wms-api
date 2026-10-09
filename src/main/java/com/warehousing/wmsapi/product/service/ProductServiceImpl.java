package com.warehousing.wmsapi.product.service;

import com.warehousing.wmsapi.product.entity.ProductEntity;
import com.warehousing.wmsapi.product.repository.ProductRepository;
import com.warehousing.wmsapi.product.dto.ProductRequest;
import com.warehousing.wmsapi.product.dto.ProductResponse;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.common.pagination.MasterPage;
import com.warehousing.wmsapi.category.entity.ProductCategoryEntity;
import com.warehousing.wmsapi.category.repository.ProductCategoryRepository;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private static final Set<String> SORT_FIELDS = Set.of("sku", "name", "createdAt");

    private final ProductRepository repository;
    private final ProductCategoryRepository categoryRepository;

    @Transactional
    @CacheEvict(cacheNames = {"products", "dashboards", "stock-summaries"}, allEntries = true)
    @Override
    public ProductResponse create(ProductRequest request) {
        ensureSkuAvailable(request.sku());
        ProductEntity product = new ProductEntity(activeCategory(request.categoryId()), request.sku(),
                request.name(), request.description(), request.unit(), request.minimumStock(), request.active());
        return ProductResponse.from(repository.save(product));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products",
            key = "#p0.page + ':' + #p0.size + ':' + #p0.sort + ':' + #p0.order + ':' + #p0.search")
    @Override
    public PageResponse<ProductResponse> list(BasePageRequest request) {
        String search = MasterPage.normalizeSearch(request.getSearch());
        var pageable = MasterPage.of(
                request.getPage(), request.getSize(), request.getSort(), request.getOrder(), SORT_FIELDS);
        var result = search == null
                ? repository.findAllByDeletedAtIsNull(pageable)
                : repository.searchActive(search, pageable);
        return new PageResponse<>(result.map(ProductResponse::from).getContent(), request.getPage(), request.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    @Override
    public ProductResponse get(UUID id) {
        return ProductResponse.from(find(id));
    }

    @Transactional
    @CacheEvict(cacheNames = {"products", "dashboards", "stock-summaries"}, allEntries = true)
    @Override
    public ProductResponse update(UUID id, ProductRequest request) {
        ProductEntity product = find(id);
        if (!product.getSku().equals(request.sku())) {
            ensureSkuAvailable(request.sku());
        }
        product.update(activeCategory(request.categoryId()), request.sku(), request.name(),
                request.description(), request.unit(), request.minimumStock(), request.active());
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(cacheNames = {"products", "dashboards", "stock-summaries"}, allEntries = true)
    @Override
    public void delete(UUID id) {
        ProductEntity product = find(id);
        if (repository.isReferenced(id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "PRODUCT_IN_USE",
                    "Product is still used by stock or transactions.");
        }
        product.markDeleted();
    }

    private ProductEntity find(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Product was not found."));
    }

    private ProductCategoryEntity activeCategory(UUID id) {
        ProductCategoryEntity category = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "INVALID_CATEGORY", "Choose an existing active product category."));
        if (!category.isActive()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "INVALID_CATEGORY", "Choose an existing active product category.");
        }
        return category;
    }

    private void ensureSkuAvailable(String sku) {
        if (repository.existsBySku(sku)) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_SKU", "Product SKU already exists.");
        }
    }
}
