package com.warehousing.wmsapi.transfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StockTransferUpdateRequest(@Size(max = 5000) String notes,
        @NotEmpty List<@Valid StockTransferItemRequest> items) {
}
