package com.warehousing.wmsapi.inbound.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.inbound.dto.InboundCreateRequest;
import com.warehousing.wmsapi.inbound.dto.InboundListItemResponse;
import com.warehousing.wmsapi.inbound.dto.InboundPutawayRequest;
import com.warehousing.wmsapi.inbound.dto.InboundRejectRequest;
import com.warehousing.wmsapi.inbound.dto.InboundResponse;
import com.warehousing.wmsapi.inbound.dto.InboundUpdateRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface InboundService {
    InboundResponse create(Authentication authentication, InboundCreateRequest request);
    PageResponse<InboundListItemResponse> list(Authentication authentication, BasePageRequest request);
    InboundResponse get(Authentication authentication, UUID id);
    InboundResponse update(Authentication authentication, UUID id, InboundUpdateRequest request);
    void delete(Authentication authentication, UUID id);
    InboundResponse submit(Authentication authentication, UUID id);
    InboundResponse approve(Authentication authentication, UUID id);
    InboundResponse reject(Authentication authentication, UUID id, InboundRejectRequest request);
    InboundResponse putaway(Authentication authentication, UUID id, InboundPutawayRequest request);
}
