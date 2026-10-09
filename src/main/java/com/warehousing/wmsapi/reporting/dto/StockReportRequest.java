package com.warehousing.wmsapi.reporting.dto;

import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockReportRequest extends BasePageRequest {
    private UUID warehouseId;
}
