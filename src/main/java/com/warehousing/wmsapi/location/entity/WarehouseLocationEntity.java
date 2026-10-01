package com.warehousing.wmsapi.location.entity;

import com.warehousing.wmsapi.common.persistence.AuditableEntity;
import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouse_locations")
public class WarehouseLocationEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private WarehouseEntity warehouse;

    @Column(nullable = false, length = 100)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected WarehouseLocationEntity() { }

    public WarehouseLocationEntity(WarehouseEntity warehouse, String code, String name,
                                   String description, boolean active) {
        this.warehouse = warehouse;
        update(code, name, description, active);
    }

    public void update(String code, String name, String description, boolean active) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public WarehouseEntity getWarehouse() { return warehouse; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}
