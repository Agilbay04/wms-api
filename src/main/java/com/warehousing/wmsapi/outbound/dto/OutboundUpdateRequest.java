package com.warehousing.wmsapi.outbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OutboundUpdateRequest(@Size(max = 5000) String notes,
        @NotEmpty List<@Valid OutboundItemRequest> items) {
}
