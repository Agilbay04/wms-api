package com.warehousing.wmsapi.outbound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OutboundRejectRequest(@NotBlank @Size(max = 5000) String rejectionNote) {
}
