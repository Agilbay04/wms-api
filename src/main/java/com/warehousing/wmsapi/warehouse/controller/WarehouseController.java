package com.warehousing.wmsapi.warehouse.controller;

import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import com.warehousing.wmsapi.warehouse.dto.WarehouseRequest;
import com.warehousing.wmsapi.warehouse.dto.WarehouseResponse;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/warehouses")
@SecurityRequirement(name = "bearerAuth")
public class WarehouseController {
    private final WarehouseService service;

    public WarehouseController(WarehouseService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSES', 'CREATE')")
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(@Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Warehouse created.", service.create(request)));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSES', 'READ')")
    public ApiResponse<PageResponse<WarehouseResponse>> list(Authentication authentication,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "code") String sort) {
        return ApiResponse.success(HttpStatus.OK, "Warehouses retrieved.", service.list(authentication, page, size, sort));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSES', 'READ')")
    public ApiResponse<WarehouseResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Warehouse retrieved.", service.get(authentication, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSES', 'UPDATE')")
    public ApiResponse<WarehouseResponse> update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody WarehouseRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Warehouse updated.", service.update(authentication, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSES', 'DELETE')")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ApiResponse.success(HttpStatus.OK, "Warehouse deleted.", null);
    }
}
