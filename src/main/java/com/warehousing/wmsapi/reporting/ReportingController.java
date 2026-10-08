package com.warehousing.wmsapi.reporting;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.inventory.StockBalanceResponse;
import com.warehousing.wmsapi.inventory.StockMovementResponse;
import com.warehousing.wmsapi.reporting.dto.DashboardResponse;
import com.warehousing.wmsapi.reporting.dto.StockReportRequest;
import com.warehousing.wmsapi.reporting.dto.StockMovementReportRequest;
import com.warehousing.wmsapi.reporting.dto.StockSummaryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
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
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class ReportingController {
    private final DashboardService dashboardService;
    private final StockReportService stockReportService;

    public ReportingController(DashboardService dashboardService, StockReportService stockReportService) {
        this.dashboardService = dashboardService;
        this.stockReportService = stockReportService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'READ')")
    public ApiResponse<DashboardResponse> dashboard(Authentication authentication,
            @RequestParam UUID warehouseId) {
        return ApiResponse.success(HttpStatus.OK, "Dashboard retrieved.",
                dashboardService.get(authentication, warehouseId));
    }

    @GetMapping("/reports/stocks/summary")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'READ')")
    public ApiResponse<StockSummaryResponse> stockSummary(Authentication authentication,
            @RequestParam UUID warehouseId) {
        return ApiResponse.success(HttpStatus.OK, "Stock summary retrieved.",
                stockReportService.summary(authentication, warehouseId));
    }

    @GetMapping("/reports/stocks")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'READ')")
    public ApiResponse<PageResponse<StockBalanceResponse>> stocks(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute StockReportRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock report retrieved.",
                stockReportService.stocks(authentication, request));
    }

    @GetMapping("/reports/movements")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'READ')")
    public ApiResponse<PageResponse<StockMovementResponse>> movements(Authentication authentication,
            @Valid @ParameterObject @ModelAttribute StockMovementReportRequest request) {
        return ApiResponse.success(HttpStatus.OK, "Stock movement report retrieved.",
                stockReportService.movements(authentication, request));
    }
}
