package com.warehousing.wmsapi.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class WarehouseAccessServiceTest {
    @Test
    void shouldReturnFalseWhenDatabaseDeniesWarehouseAccess() {
        AccessControlRepository repository = mock(AccessControlRepository.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("staff@example.com");
        UUID warehouseId = UUID.randomUUID();
        when(repository.canAccessWarehouse("staff@example.com", warehouseId)).thenReturn(false);

        assertFalse(new WarehouseAccessServiceImpl(repository).canAccessWarehouse(authentication, warehouseId));
    }
}
