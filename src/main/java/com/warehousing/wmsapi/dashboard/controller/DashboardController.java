package com.warehousing.wmsapi.dashboard.controller;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.dashboard.dto.DashboardResponse;
import com.warehousing.wmsapi.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Dashboard", description = "Warehouse stock and pending-work summaries.")
@RequestMapping("/api/v1/dashboard")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'READ')")
    public ApiResponse<DashboardResponse> get(Authentication authentication, @RequestParam UUID warehouseId) {
        return ApiResponse.success(HttpStatus.OK, "Dashboard retrieved.",
                dashboardService.get(authentication, warehouseId));
    }
}
