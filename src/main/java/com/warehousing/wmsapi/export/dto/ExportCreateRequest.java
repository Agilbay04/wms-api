package com.warehousing.wmsapi.export.dto;

import com.warehousing.wmsapi.export.enums.ExportReportType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ExportCreateRequest(@NotNull UUID warehouseId, @NotNull ExportReportType reportType,
        LocalDate fromDate, LocalDate toDate) {
}
