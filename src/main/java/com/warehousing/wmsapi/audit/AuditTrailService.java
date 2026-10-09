package com.warehousing.wmsapi.audit;

import com.warehousing.wmsapi.audit.dto.AuditTrailRequest;
import com.warehousing.wmsapi.audit.dto.AuditTrailResponse;
import com.warehousing.wmsapi.audit.repository.AuditTrailRepository;
import com.warehousing.wmsapi.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditTrailService {
    private final AuditTrailRepository repository;

    public PageResponse<AuditTrailResponse> list(Authentication authentication, AuditTrailRequest request) {
        return repository.list(authentication, request);
    }
}
