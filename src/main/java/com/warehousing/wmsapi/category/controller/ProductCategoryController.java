package com.warehousing.wmsapi.category.controller;

import com.warehousing.wmsapi.category.service.ProductCategoryService;
import com.warehousing.wmsapi.category.dto.CategoryRequest;
import com.warehousing.wmsapi.category.dto.CategoryResponse;

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
@Tag(name = "Product categories", description = "Maintain product categories.")
@RequestMapping("/api/v1/product-categories")
@SecurityRequirement(name = "bearerAuth")
public class ProductCategoryController {
    private final ProductCategoryService service;

    public ProductCategoryController(ProductCategoryService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'PRODUCT_CATEGORIES', 'CREATE')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Product category created.", service.create(request)));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'PRODUCT_CATEGORIES', 'READ')")
    public ApiResponse<PageResponse<CategoryResponse>> list(
            @Valid @ParameterObject @ModelAttribute BasePageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Product categories retrieved.", service.list(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'PRODUCT_CATEGORIES', 'READ')")
    public ApiResponse<CategoryResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Product category retrieved.", service.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'PRODUCT_CATEGORIES', 'UPDATE')")
    public ApiResponse<CategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Product category updated.", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'PRODUCT_CATEGORIES', 'DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.success(HttpStatus.OK, "Product category deleted.", null);
    }
}
