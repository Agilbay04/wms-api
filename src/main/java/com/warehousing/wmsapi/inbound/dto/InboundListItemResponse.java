package com.warehousing.wmsapi.inbound.dto;

import com.warehousing.wmsapi.inbound.enums.InboundStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InboundListItemResponse(UUID id, UUID warehouseId, String referenceNumber,
        String purchaseOrderNumber, InboundStatus status, String notes, String rejectionNote,
        OffsetDateTime submittedAt, OffsetDateTime reviewedAt, UUID createdByUserId,
        UUID reviewedByUserId, OffsetDateTime createdAt, int itemCount) {
}
