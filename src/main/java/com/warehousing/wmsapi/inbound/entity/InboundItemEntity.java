package com.warehousing.wmsapi.inbound.entity;

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
@Table(name = "inbound_items", uniqueConstraints =
        @UniqueConstraint(name = "uq_inbound_items_inbound_product", columnNames = {"inbound_id", "product_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboundItemEntity extends AuditableEntity {
    @Column(name = "inbound_id", nullable = false)
    private UUID inboundId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(columnDefinition = "text")
    private String notes;
}
