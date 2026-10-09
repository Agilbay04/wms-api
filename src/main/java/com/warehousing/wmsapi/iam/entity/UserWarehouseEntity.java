package com.warehousing.wmsapi.iam.entity;

import com.warehousing.wmsapi.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_warehouses", uniqueConstraints =
        @UniqueConstraint(name = "uq_user_warehouses_user_warehouse", columnNames = {"user_id", "warehouse_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWarehouseEntity extends AuditableEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;
}
