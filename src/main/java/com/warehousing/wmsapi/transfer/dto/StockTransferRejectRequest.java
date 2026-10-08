package com.warehousing.wmsapi.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StockTransferRejectRequest(@NotBlank @Size(max = 5000) String rejectionNote) {
}
