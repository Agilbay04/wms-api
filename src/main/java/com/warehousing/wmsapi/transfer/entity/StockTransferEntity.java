package com.warehousing.wmsapi.transfer.entity;

import com.warehousing.wmsapi.common.persistence.AuditableEntity;
import com.warehousing.wmsapi.transfer.enums.StockTransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_transfers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTransferEntity extends AuditableEntity {
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "reference_number", nullable = false, unique = true, length = 100)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockTransferStatus status;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "rejection_note", columnDefinition = "text")
    private String rejectionNote;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;
}
