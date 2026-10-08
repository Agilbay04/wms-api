package com.warehousing.wmsapi.inbound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InboundRejectRequest(@NotBlank @Size(max = 5000) String rejectionNote) {
}
