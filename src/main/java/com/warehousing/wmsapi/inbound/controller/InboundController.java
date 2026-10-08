package com.warehousing.wmsapi.inbound.controller;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.inbound.dto.InboundCreateRequest;
import com.warehousing.wmsapi.inbound.dto.InboundListItemResponse;
import com.warehousing.wmsapi.inbound.dto.InboundPutawayRequest;
import com.warehousing.wmsapi.inbound.dto.InboundRejectRequest;
import com.warehousing.wmsapi.inbound.dto.InboundResponse;
import com.warehousing.wmsapi.inbound.dto.InboundUpdateRequest;
import com.warehousing.wmsapi.inbound.service.InboundService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Inbounds", description = "Receive stock through draft, approval, and putaway workflows.")
@RequestMapping("/api/v1/inbounds")
@SecurityRequirement(name = "bearerAuth")
public class InboundController {
    private final InboundService service;

    public InboundController(InboundService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INBOUNDS', 'CREATE')")
    public ResponseEntity<ApiResponse<InboundResponse>> create(Authentication authentication,
            @Valid @RequestBody InboundCreateRequest request) {
        InboundResponse inbound = service.create(authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Inbound draft created.", inbound));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INBOUNDS', 'READ')")
    public ApiResponse<PageResponse<InboundListItemResponse>> list(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute BasePageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Inbounds retrieved.", service.list(authentication, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INBOUNDS', 'READ')")
    public ApiResponse<InboundResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Inbound retrieved.", service.get(authentication, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INBOUNDS', 'UPDATE')")
    public ApiResponse<InboundResponse> update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody InboundUpdateRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Inbound updated.", service.update(authentication, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INBOUNDS', 'DELETE')")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ApiResponse.success(HttpStatus.OK, "Inbound draft deleted.", null);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INBOUNDS', 'UPDATE')")
    public ApiResponse<InboundResponse> submit(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Inbound submitted for approval.", service.submit(authentication, id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INBOUNDS', 'APPROVE')")
    public ApiResponse<InboundResponse> approve(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Inbound approved.", service.approve(authentication, id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INBOUNDS', 'REJECT')")
    public ApiResponse<InboundResponse> reject(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody InboundRejectRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Inbound rejected.", service.reject(authentication, id, request));
    }

    @PostMapping("/{id}/putaway")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INBOUNDS', 'UPDATE')")
    public ApiResponse<InboundResponse> putaway(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody InboundPutawayRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Inbound putaway completed.",
                service.putaway(authentication, id, request));
    }
}
