package com.warehousing.wmsapi.adjustment.dto;

import com.warehousing.wmsapi.adjustment.StockAdjustmentStatus;
import com.warehousing.wmsapi.adjustment.StockAdjustmentType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockAdjustmentResponse(UUID id, UUID warehouseId, String referenceNumber,
        StockAdjustmentStatus status, StockAdjustmentType adjustmentType, String reason,
        String rejectionNote, OffsetDateTime submittedAt, OffsetDateTime reviewedAt,
        UUID createdByUserId, UUID reviewedByUserId, OffsetDateTime createdAt,
        List<StockAdjustmentItemResponse> items) {
}
