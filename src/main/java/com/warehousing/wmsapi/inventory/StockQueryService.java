package com.warehousing.wmsapi.inventory;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.inventory.repository.StockQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockQueryService {
    private final StockQueryRepository repository;

    public PageResponse<StockBalanceResponse> stocks(Authentication authentication, BasePageRequest request) {
        return repository.stocks(authentication, request);
    }

    public PageResponse<StockMovementResponse> movements(Authentication authentication, BasePageRequest request) {
        return repository.movements(authentication, request);
    }
}
