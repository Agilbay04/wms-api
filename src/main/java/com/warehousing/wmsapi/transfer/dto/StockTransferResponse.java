package com.warehousing.wmsapi.transfer.dto;

import com.warehousing.wmsapi.transfer.StockTransferStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockTransferResponse(UUID id, UUID warehouseId, String referenceNumber,
        StockTransferStatus status, String notes, String rejectionNote, OffsetDateTime submittedAt,
        OffsetDateTime reviewedAt, UUID createdByUserId, UUID reviewedByUserId,
        OffsetDateTime createdAt, List<StockTransferItemResponse> items) {
}
