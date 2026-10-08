package com.warehousing.wmsapi.warehouse.dto;

import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WarehouseResponse(UUID id, String code, String name, String address, boolean active,
                                OffsetDateTime createdAt)
        implements Serializable {
    public static WarehouseResponse from(WarehouseEntity entity) {
        return new WarehouseResponse(entity.getId(), entity.getCode(), entity.getName(),
                entity.getAddress(), entity.isActive(), entity.getCreatedAt());
    }
}
