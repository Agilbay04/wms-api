package com.warehousing.wmsapi.reporting.dto;

import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import java.util.UUID;

public class StockReportRequest extends BasePageRequest {
    private UUID warehouseId;

    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
}
