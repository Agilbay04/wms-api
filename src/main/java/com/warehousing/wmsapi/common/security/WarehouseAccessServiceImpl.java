package com.warehousing.wmsapi.common.security;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("warehouseAccessService")
@RequiredArgsConstructor
public class WarehouseAccessServiceImpl implements WarehouseAccessService {
    private final AccessControlRepository repository;

    @Override
    public boolean canAccessWarehouse(Authentication authentication, UUID warehouseId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return repository.canAccessWarehouse(authentication.getName(), warehouseId);
    }
}
