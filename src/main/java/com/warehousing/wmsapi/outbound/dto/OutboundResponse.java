package com.warehousing.wmsapi.outbound.dto;

import com.warehousing.wmsapi.outbound.OutboundStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OutboundResponse(UUID id, UUID warehouseId, String referenceNumber, OutboundStatus status,
        String notes, String rejectionNote, OffsetDateTime submittedAt, OffsetDateTime reviewedAt,
        UUID createdByUserId, UUID reviewedByUserId, OffsetDateTime createdAt, List<OutboundItemResponse> items) {
}
