package com.warehousing.wmsapi.location.dto;

import com.warehousing.wmsapi.location.entity.WarehouseLocationEntity;

import java.io.Serializable;
import java.util.UUID;

public record LocationResponse(UUID id, UUID warehouseId, String code, String name,
                               String description, boolean active) implements Serializable {
    public static LocationResponse from(WarehouseLocationEntity entity) {
        return new LocationResponse(entity.getId(), entity.getWarehouse().getId(), entity.getCode(),
                entity.getName(), entity.getDescription(), entity.isActive());
    }
}
