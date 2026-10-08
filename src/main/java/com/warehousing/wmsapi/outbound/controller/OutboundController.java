package com.warehousing.wmsapi.outbound.controller;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.outbound.dto.OutboundCreateRequest;
import com.warehousing.wmsapi.outbound.dto.OutboundListItemResponse;
import com.warehousing.wmsapi.outbound.dto.OutboundRejectRequest;
import com.warehousing.wmsapi.outbound.dto.OutboundResponse;
import com.warehousing.wmsapi.outbound.dto.OutboundUpdateRequest;
import com.warehousing.wmsapi.outbound.service.OutboundService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outbounds")
@SecurityRequirement(name = "bearerAuth")
public class OutboundController {
    private final OutboundService service;

    public OutboundController(OutboundService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'OUTBOUNDS', 'CREATE')")
    public ResponseEntity<ApiResponse<OutboundResponse>> create(Authentication authentication,
            @Valid @RequestBody OutboundCreateRequest request) {
        OutboundResponse outbound = service.create(authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Outbound draft created.", outbound));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'OUTBOUNDS', 'READ')")
    public ApiResponse<PageResponse<OutboundListItemResponse>> list(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute BasePageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Outbounds retrieved.", service.list(authentication, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'OUTBOUNDS', 'READ')")
    public ApiResponse<OutboundResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Outbound retrieved.", service.get(authentication, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'OUTBOUNDS', 'UPDATE')")
    public ApiResponse<OutboundResponse> update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody OutboundUpdateRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Outbound updated.", service.update(authentication, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'OUTBOUNDS', 'DELETE')")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ApiResponse.success(HttpStatus.OK, "Outbound draft deleted.", null);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'OUTBOUNDS', 'UPDATE')")
    public ApiResponse<OutboundResponse> submit(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Outbound submitted for approval.", service.submit(authentication, id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'OUTBOUNDS', 'APPROVE')")
    public ApiResponse<OutboundResponse> approve(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Outbound approved.", service.approve(authentication, id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'OUTBOUNDS', 'REJECT')")
    public ApiResponse<OutboundResponse> reject(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody OutboundRejectRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Outbound rejected.", service.reject(authentication, id, request));
    }
}
