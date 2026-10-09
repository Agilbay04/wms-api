package com.warehousing.wmsapi.dashboard.service;

import com.warehousing.wmsapi.dashboard.dto.DashboardResponse;
import com.warehousing.wmsapi.dashboard.repository.DashboardRepository;
import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final WarehouseService warehouseService;
    private final DashboardRepository dashboardRepository;

    public DashboardResponse get(Authentication authentication, UUID warehouseId) {
        warehouseService.requireAccess(authentication, warehouseId);
        return dashboardRepository.get(warehouseId);
    }
}
