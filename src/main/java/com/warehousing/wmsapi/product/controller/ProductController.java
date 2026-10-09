package com.warehousing.wmsapi.product.controller;

import com.warehousing.wmsapi.product.service.ProductService;
import com.warehousing.wmsapi.product.dto.ProductRequest;
import com.warehousing.wmsapi.product.dto.ProductResponse;

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
@Tag(name = "Products", description = "Maintain the product catalog.")
@RequestMapping("/api/v1/products")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'PRODUCTS', 'CREATE')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Product created.", service.create(request)));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'PRODUCTS', 'READ')")
    public ApiResponse<PageResponse<ProductResponse>> list(
            @Valid @ParameterObject @ModelAttribute BasePageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Products retrieved.", service.list(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'PRODUCTS', 'READ')")
    public ApiResponse<ProductResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Product retrieved.", service.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'PRODUCTS', 'UPDATE')")
    public ApiResponse<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Product updated.", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'PRODUCTS', 'DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.success(HttpStatus.OK, "Product deleted.", null);
    }
}
