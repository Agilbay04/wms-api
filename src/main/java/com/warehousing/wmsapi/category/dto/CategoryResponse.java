package com.warehousing.wmsapi.category.dto;

import com.warehousing.wmsapi.category.entity.ProductCategoryEntity;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoryResponse(UUID id, String code, String name, String description, boolean active,
                               OffsetDateTime createdAt)
        implements Serializable {
    public static CategoryResponse from(ProductCategoryEntity entity) {
        return new CategoryResponse(entity.getId(), entity.getCode(), entity.getName(),
                entity.getDescription(), entity.isActive(), entity.getCreatedAt());
    }
}
