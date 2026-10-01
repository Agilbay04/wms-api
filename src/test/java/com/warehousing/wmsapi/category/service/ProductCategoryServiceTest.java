package com.warehousing.wmsapi.category.service;

import com.warehousing.wmsapi.category.entity.ProductCategoryEntity;
import com.warehousing.wmsapi.category.repository.ProductCategoryRepository;
import com.warehousing.wmsapi.category.dto.CategoryRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.product.repository.ProductRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductCategoryServiceTest {
    @Test
    void rejectsDeletingCategoryUsedByLiveProduct() {
        UUID id = UUID.randomUUID();
        ProductCategoryRepository categories = mock(ProductCategoryRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        when(categories.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(
                new ProductCategoryEntity("CAT", "Category", null, true)));
        when(products.existsByCategory_IdAndDeletedAtIsNull(id)).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new ProductCategoryServiceImpl(categories, products).delete(id));
        assertEquals("CATEGORY_IN_USE", error.getCode());
    }

    @Test
    void rejectsDeactivatingCategoryUsedByLiveProduct() {
        UUID id = UUID.randomUUID();
        ProductCategoryRepository categories = mock(ProductCategoryRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        when(categories.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(
                new ProductCategoryEntity("CAT", "Category", null, true)));
        when(products.existsByCategory_IdAndDeletedAtIsNull(id)).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new ProductCategoryServiceImpl(categories, products).update(id,
                        new CategoryRequest("CAT", "Category", null, false)));
        assertEquals("CATEGORY_IN_USE", error.getCode());
    }
}
