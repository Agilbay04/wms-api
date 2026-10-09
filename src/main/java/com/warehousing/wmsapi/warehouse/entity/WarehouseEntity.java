package com.warehousing.wmsapi.warehouse.entity;

import com.warehousing.wmsapi.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "warehouses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseEntity extends AuditableEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "text")
    private String address;
    @Column(name = "is_active", nullable = false)
    private boolean active;

    public WarehouseEntity(String code, String name, String address, boolean active) {
        update(code, name, address, active);
    }

    public void update(String code, String name, String address, boolean active) {
        this.code = code;
        this.name = name;
        this.address = address;
        this.active = active;
    }

}
