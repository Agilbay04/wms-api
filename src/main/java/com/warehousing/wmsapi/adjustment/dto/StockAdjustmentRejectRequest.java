package com.warehousing.wmsapi.adjustment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRejectRequest(@NotBlank @Size(max = 5000) String rejectionNote) {
}
