package com.warehousing.wmsapi.transfer.controller;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.transfer.dto.StockTransferCreateRequest;
import com.warehousing.wmsapi.transfer.dto.StockTransferListItemResponse;
import com.warehousing.wmsapi.transfer.dto.StockTransferRejectRequest;
import com.warehousing.wmsapi.transfer.dto.StockTransferResponse;
import com.warehousing.wmsapi.transfer.dto.StockTransferUpdateRequest;
import com.warehousing.wmsapi.transfer.service.StockTransferService;
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
@RequestMapping("/api/v1/stock-transfers")
@SecurityRequirement(name = "bearerAuth")
public class StockTransferController {
    private final StockTransferService service;

    public StockTransferController(StockTransferService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_TRANSFERS', 'CREATE')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> create(Authentication authentication,
            @Valid @RequestBody StockTransferCreateRequest request) {
        StockTransferResponse transfer = service.create(authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Stock transfer draft created.", transfer));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_TRANSFERS', 'READ')")
    public ApiResponse<PageResponse<StockTransferListItemResponse>> list(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute BasePageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock transfers retrieved.", service.list(authentication, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_TRANSFERS', 'READ')")
    public ApiResponse<StockTransferResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Stock transfer retrieved.", service.get(authentication, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_TRANSFERS', 'UPDATE')")
    public ApiResponse<StockTransferResponse> update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody StockTransferUpdateRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock transfer updated.", service.update(authentication, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_TRANSFERS', 'DELETE')")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ApiResponse.success(HttpStatus.OK, "Stock transfer draft deleted.", null);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_TRANSFERS', 'UPDATE')")
    public ApiResponse<StockTransferResponse> submit(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Stock transfer submitted for approval.", service.submit(authentication, id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_TRANSFERS', 'APPROVE')")
    public ApiResponse<StockTransferResponse> approve(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Stock transfer approved.", service.approve(authentication, id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'STOCK_TRANSFERS', 'REJECT')")
    public ApiResponse<StockTransferResponse> reject(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody StockTransferRejectRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock transfer rejected.", service.reject(authentication, id, request));
    }
}
