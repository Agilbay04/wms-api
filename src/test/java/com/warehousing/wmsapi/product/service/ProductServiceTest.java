package com.warehousing.wmsapi.product.service;

import com.warehousing.wmsapi.product.repository.ProductRepository;
import com.warehousing.wmsapi.product.dto.ProductRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.category.entity.ProductCategoryEntity;
import com.warehousing.wmsapi.category.repository.ProductCategoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ProductServiceTest {
    @Test
    void rejectsProductWithInactiveCategory() {
        ProductRepository products = mock(ProductRepository.class);
        ProductCategoryRepository categories = mock(ProductCategoryRepository.class);
        UUID categoryId = UUID.randomUUID();
        when(categories.findByIdAndDeletedAtIsNull(categoryId)).thenReturn(Optional.of(
                new ProductCategoryEntity("CAT", "Category", null, false)));
        ProductService service = new ProductServiceImpl(products, categories, mock(JdbcTemplate.class));
        ProductRequest request = new ProductRequest(categoryId, "SKU-1", "Product", null, "pcs", 0, true);

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(request));
        assertEquals("INVALID_CATEGORY", error.getCode());
    }

    @Test
    void rejectsDuplicateSkuBeforeSaving() {
        ProductRepository products = mock(ProductRepository.class);
        when(products.existsBySku("SKU-1")).thenReturn(true);
        ProductService service = new ProductServiceImpl(products, mock(ProductCategoryRepository.class),
                mock(JdbcTemplate.class));

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(
                new ProductRequest(UUID.randomUUID(), "SKU-1", "Product", null, "pcs", 0, true)));
        assertEquals("DUPLICATE_SKU", error.getCode());
    }
}
