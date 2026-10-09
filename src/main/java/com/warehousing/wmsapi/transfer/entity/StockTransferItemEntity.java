package com.warehousing.wmsapi.transfer.entity;

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
@Table(name = "stock_transfer_items", uniqueConstraints =
        @UniqueConstraint(name = "uq_stock_transfer_items_transfer_product", columnNames = {"stock_transfer_id", "product_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTransferItemEntity extends AuditableEntity {
    @Column(name = "stock_transfer_id", nullable = false)
    private UUID stockTransferId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "source_warehouse_location_id", nullable = false)
    private UUID sourceWarehouseLocationId;

    @Column(name = "destination_warehouse_location_id", nullable = false)
    private UUID destinationWarehouseLocationId;

    @Column(nullable = false)
    private int quantity;

    @Column(columnDefinition = "text")
    private String notes;
}
