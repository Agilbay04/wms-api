package com.warehousing.wmsapi.reporting.controller;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.inventory.StockBalanceResponse;
import com.warehousing.wmsapi.inventory.StockMovementResponse;
import com.warehousing.wmsapi.reporting.dto.StockReportRequest;
import com.warehousing.wmsapi.reporting.dto.StockMovementReportRequest;
import com.warehousing.wmsapi.reporting.dto.StockSummaryResponse;
import com.warehousing.wmsapi.reporting.service.StockReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Reports", description = "Query stock summaries, balances, and movement history.")
@RequestMapping("/api/v1/reports")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ReportingController {
    private final StockReportService stockReportService;

    @GetMapping("/stocks/summary")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'READ')")
    public ApiResponse<StockSummaryResponse> stockSummary(Authentication authentication,
            @RequestParam UUID warehouseId) {
        return ApiResponse.success(HttpStatus.OK, "Stock summary retrieved.",
                stockReportService.summary(authentication, warehouseId));
    }

    @GetMapping("/stocks")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'READ')")
    public ApiResponse<PageResponse<StockBalanceResponse>> stocks(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute StockReportRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock report retrieved.",
                stockReportService.stocks(authentication, request));
    }

    @GetMapping("/movements")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'READ')")
    public ApiResponse<PageResponse<StockMovementResponse>> movements(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute StockMovementReportRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock movement report retrieved.",
                stockReportService.movements(authentication, request));
    }
}
