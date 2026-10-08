package com.warehousing.wmsapi.location.controller;

import com.warehousing.wmsapi.location.service.WarehouseLocationService;
import com.warehousing.wmsapi.location.dto.LocationRequest;
import com.warehousing.wmsapi.location.dto.LocationResponse;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
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
@Tag(name = "Warehouse locations", description = "Maintain locations within a warehouse.")
@RequestMapping("/api/v1/warehouses/{warehouseId}/locations")
@SecurityRequirement(name = "bearerAuth")
public class WarehouseLocationController {
    private final WarehouseLocationService service;

    public WarehouseLocationController(WarehouseLocationService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSE_LOCATIONS', 'CREATE')")
    public ResponseEntity<ApiResponse<LocationResponse>> create(Authentication authentication,
            @PathVariable UUID warehouseId, @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED,
                "Warehouse location created.", service.create(authentication, warehouseId, request)));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSE_LOCATIONS', 'READ')")
    public ApiResponse<PageResponse<LocationResponse>> list(Authentication authentication,
            @PathVariable UUID warehouseId, @Valid @ParameterObject @ModelAttribute BasePageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Warehouse locations retrieved.",
                service.list(authentication, warehouseId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSE_LOCATIONS', 'READ')")
    public ApiResponse<LocationResponse> get(Authentication authentication, @PathVariable UUID warehouseId,
            @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Warehouse location retrieved.",
                service.get(authentication, warehouseId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSE_LOCATIONS', 'UPDATE')")
    public ApiResponse<LocationResponse> update(Authentication authentication, @PathVariable UUID warehouseId,
            @PathVariable UUID id, @Valid @RequestBody LocationRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Warehouse location updated.",
                service.update(authentication, warehouseId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'WAREHOUSE_LOCATIONS', 'DELETE')")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable UUID warehouseId,
            @PathVariable UUID id) {
        service.delete(authentication, warehouseId, id);
        return ApiResponse.success(HttpStatus.OK, "Warehouse location deleted.", null);
    }
}
