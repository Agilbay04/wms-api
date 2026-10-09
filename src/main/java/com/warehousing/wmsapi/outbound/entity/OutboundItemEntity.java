package com.warehousing.wmsapi.outbound.entity;

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
@Table(name = "outbound_items", uniqueConstraints =
        @UniqueConstraint(name = "uq_outbound_items_outbound_product", columnNames = {"outbound_id", "product_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboundItemEntity extends AuditableEntity {
    @Column(name = "outbound_id", nullable = false)
    private UUID outboundId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "warehouse_location_id", nullable = false)
    private UUID warehouseLocationId;

    @Column(nullable = false)
    private int quantity;

    @Column(columnDefinition = "text")
    private String notes;
}
