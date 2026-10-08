package com.warehousing.wmsapi.export.dto;

import com.warehousing.wmsapi.export.enums.ExportJobStatus;
import com.warehousing.wmsapi.export.enums.ExportReportType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExportJobResponse(UUID id, UUID warehouseId, ExportReportType reportType,
        ExportJobStatus status, String errorMessage, int attemptCount, OffsetDateTime createdAt,
        OffsetDateTime completedAt) {
}
