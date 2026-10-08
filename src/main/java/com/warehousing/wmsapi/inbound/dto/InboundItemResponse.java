package com.warehousing.wmsapi.inbound.dto;

import java.util.UUID;

public record InboundItemResponse(UUID id, UUID productId, String productSku, String productName,
                                 int quantity, String notes) {
}
