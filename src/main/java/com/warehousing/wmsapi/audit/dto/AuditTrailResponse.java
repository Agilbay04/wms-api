package com.warehousing.wmsapi.audit.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditTrailResponse(UUID id, UUID userId, String userName, String userEmail,
        String action, String entityType, UUID entityId, String description, OffsetDateTime occurredAt) {
}
