package com.warehousing.wmsapi.adjustment.controller;

import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentCreateRequest;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentListItemResponse;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentRejectRequest;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentResponse;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentUpdateRequest;
import com.warehousing.wmsapi.adjustment.service.StockAdjustmentService;
import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
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
@RequestMapping("/api/v1/stock-adjustments")
@SecurityRequirement(name = "bearerAuth")
public class StockAdjustmentController {
    private final StockAdjustmentService service;

    public StockAdjustmentController(StockAdjustmentService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_ADJUSTMENTS', 'CREATE')")
    public ResponseEntity<ApiResponse<StockAdjustmentResponse>> create(Authentication authentication,
            @Valid @RequestBody StockAdjustmentCreateRequest request) {
        StockAdjustmentResponse adjustment = service.create(authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Stock adjustment draft created.", adjustment));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_ADJUSTMENTS', 'READ')")
    public ApiResponse<PageResponse<StockAdjustmentListItemResponse>> list(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute BasePageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock adjustments retrieved.", service.list(authentication, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_ADJUSTMENTS', 'READ')")
    public ApiResponse<StockAdjustmentResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Stock adjustment retrieved.", service.get(authentication, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_ADJUSTMENTS', 'UPDATE')")
    public ApiResponse<StockAdjustmentResponse> update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody StockAdjustmentUpdateRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock adjustment updated.", service.update(authentication, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_ADJUSTMENTS', 'DELETE')")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ApiResponse.success(HttpStatus.OK, "Stock adjustment draft deleted.", null);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_ADJUSTMENTS', 'UPDATE')")
    public ApiResponse<StockAdjustmentResponse> submit(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Stock adjustment submitted for approval.", service.submit(authentication, id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_ADJUSTMENTS', 'APPROVE')")
    public ApiResponse<StockAdjustmentResponse> approve(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Stock adjustment approved.", service.approve(authentication, id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_ADJUSTMENTS', 'REJECT')")
    public ApiResponse<StockAdjustmentResponse> reject(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody StockAdjustmentRejectRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock adjustment rejected.", service.reject(authentication, id, request));
    }
}
