package com.warehousing.wmsapi.reporting.dto;

import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public class StockMovementReportRequest extends BasePageRequest {
    private UUID warehouseId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Inclusive start date in UTC.")
    private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Inclusive end date in UTC.")
    private LocalDate toDate;

    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
}
