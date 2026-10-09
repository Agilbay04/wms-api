package com.warehousing.wmsapi.reporting.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.inventory.StockBalanceResponse;
import com.warehousing.wmsapi.inventory.StockMovementResponse;
import com.warehousing.wmsapi.reporting.dto.StockMovementReportRequest;
import com.warehousing.wmsapi.reporting.dto.StockReportRequest;
import com.warehousing.wmsapi.reporting.dto.StockSummaryResponse;
import com.warehousing.wmsapi.reporting.repository.StockReportRepository;
import com.warehousing.wmsapi.reporting.repository.StockSummaryRepository;
import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockReportService {
    private final StockReportRepository repository;
    private final StockSummaryRepository summaryRepository;
    private final WarehouseService warehouseService;

    public StockSummaryResponse summary(Authentication authentication, UUID warehouseId) {
        warehouseService.requireAccess(authentication, warehouseId);
        return summaryRepository.get(warehouseId);
    }

    public PageResponse<StockBalanceResponse> stocks(Authentication authentication, StockReportRequest request) {
        requireWarehouseAccess(authentication, request.getWarehouseId());
        return repository.stocks(authentication, request);
    }

    public PageResponse<StockMovementResponse> movements(Authentication authentication,
                                                           StockMovementReportRequest request) {
        requireWarehouseAccess(authentication, request.getWarehouseId());
        return repository.movements(authentication, request);
    }

    private void requireWarehouseAccess(Authentication authentication, UUID warehouseId) {
        if (warehouseId != null) warehouseService.requireAccess(authentication, warehouseId);
    }
}
