package com.warehousing.wmsapi.warehouse.entity;

import com.warehousing.wmsapi.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouses")
public class WarehouseEntity extends AuditableEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "text")
    private String address;
    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected WarehouseEntity() { }

    public WarehouseEntity(String code, String name, String address, boolean active) {
        update(code, name, address, active);
    }

    public void update(String code, String name, String address, boolean active) {
        this.code = code;
        this.name = name;
        this.address = address;
        this.active = active;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
}
