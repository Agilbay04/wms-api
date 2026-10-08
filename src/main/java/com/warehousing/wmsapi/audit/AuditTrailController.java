package com.warehousing.wmsapi.audit;

import com.warehousing.wmsapi.audit.dto.AuditTrailRequest;
import com.warehousing.wmsapi.audit.dto.AuditTrailResponse;
import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-trails")
@SecurityRequirement(name = "bearerAuth")
public class AuditTrailController {
    private final AuditTrailService service;

    public AuditTrailController(AuditTrailService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'READ')")
    public ApiResponse<PageResponse<AuditTrailResponse>> list(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute AuditTrailRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Audit trails retrieved.", service.list(authentication, request));
    }
}
