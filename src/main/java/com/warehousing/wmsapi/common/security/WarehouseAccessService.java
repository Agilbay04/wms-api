package com.warehousing.wmsapi.common.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface WarehouseAccessService {
    boolean canAccessWarehouse(Authentication authentication, UUID warehouseId);
}
