package com.warehousing.wmsapi.reporting;

import com.warehousing.wmsapi.reporting.dto.DashboardResponse;
import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final WarehouseService warehouseService;
    private final DashboardCache dashboardCache;

    public DashboardService(WarehouseService warehouseService, DashboardCache dashboardCache) {
        this.warehouseService = warehouseService;
        this.dashboardCache = dashboardCache;
    }

    public DashboardResponse get(Authentication authentication, UUID warehouseId) {
        warehouseService.requireAccess(authentication, warehouseId);
        return dashboardCache.get(warehouseId);
    }
}
