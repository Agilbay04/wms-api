package com.warehousing.wmsapi.adjustment.entity;

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
@Table(name = "stock_adjustment_items", uniqueConstraints =
        @UniqueConstraint(name = "uq_stock_adjustment_items_adjustment_product", columnNames = {"stock_adjustment_id", "product_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockAdjustmentItemEntity extends AuditableEntity {
    @Column(name = "stock_adjustment_id", nullable = false)
    private UUID stockAdjustmentId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "warehouse_location_id", nullable = false)
    private UUID warehouseLocationId;

    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(columnDefinition = "text")
    private String notes;
}
