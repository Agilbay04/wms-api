package com.warehousing.wmsapi.adjustment.dto;

import com.warehousing.wmsapi.adjustment.enums.StockAdjustmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record StockAdjustmentCreateRequest(@NotNull UUID warehouseId, @NotNull StockAdjustmentType adjustmentType,
        @NotBlank @Size(max = 5000) String reason,
        @NotEmpty List<@Valid StockAdjustmentItemRequest> items) {
}
