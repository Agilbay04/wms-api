package com.warehousing.wmsapi.adjustment.dto;

import com.warehousing.wmsapi.adjustment.enums.StockAdjustmentStatus;
import com.warehousing.wmsapi.adjustment.enums.StockAdjustmentType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockAdjustmentListItemResponse(UUID id, UUID warehouseId, String referenceNumber,
        StockAdjustmentStatus status, StockAdjustmentType adjustmentType, String reason,
        String rejectionNote, OffsetDateTime submittedAt, OffsetDateTime reviewedAt,
        UUID createdByUserId, UUID reviewedByUserId, OffsetDateTime createdAt, int itemCount) {
}
