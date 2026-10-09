package com.warehousing.wmsapi.warehouse.controller;

import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import com.warehousing.wmsapi.warehouse.dto.WarehouseRequest;
import com.warehousing.wmsapi.warehouse.dto.WarehouseResponse;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Warehouses", description = "Maintain warehouses and view warehouses accessible to the user.")
@RequestMapping("/api/v1/warehouses")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WarehouseController {
    private final WarehouseService service;

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSES', 'CREATE')")
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(@Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Warehouse created.", service.create(request)));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSES', 'READ')")
    public ApiResponse<PageResponse<WarehouseResponse>> list(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute BasePageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Warehouses retrieved.",
                service.list(authentication, request));
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
