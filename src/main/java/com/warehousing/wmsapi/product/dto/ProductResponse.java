package com.warehousing.wmsapi.product.dto;

import com.warehousing.wmsapi.product.entity.ProductEntity;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id, UUID categoryId, String sku, String name, String description,
        String unit, int minimumStock, boolean active, OffsetDateTime createdAt
) implements Serializable {
    public static ProductResponse from(ProductEntity entity) {
        return new ProductResponse(entity.getId(), entity.getCategory().getId(), entity.getSku(),
                entity.getName(), entity.getDescription(), entity.getUnit(),
                entity.getMinimumStock(), entity.isActive(), entity.getCreatedAt());
    }
}
