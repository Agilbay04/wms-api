package com.warehousing.wmsapi.inbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InboundPutawayRequest(@NotEmpty List<@Valid InboundPutawayItemRequest> items) {
}
