package com.warehousing.wmsapi.inventory;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Inventory", description = "Query warehouse stock balances and stock movements.")
@RequestMapping("/api/v1/inventory")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class StockMovementController {
    private final StockQueryService queryService;

    @GetMapping("/stocks")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INVENTORY', 'READ')")
    public ApiResponse<PageResponse<StockBalanceResponse>> stocks(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute BasePageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock balances retrieved.",
                queryService.stocks(authentication, request));
    }

    @GetMapping("/movements")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'INVENTORY', 'READ')")
    public ApiResponse<PageResponse<StockMovementResponse>> movements(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute BasePageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock movements retrieved.",
                queryService.movements(authentication, request));
    }
}
