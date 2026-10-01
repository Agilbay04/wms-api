package com.warehousing.wmsapi.location.service;

import com.warehousing.wmsapi.location.repository.WarehouseLocationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;

class WarehouseLocationServiceTest {
    @Test
    void doesNotReturnLocationOutsideRequestedWarehouse() {
        UUID warehouseId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        WarehouseLocationRepository repository = mock(WarehouseLocationRepository.class);
        WarehouseService warehouses = mock(WarehouseService.class);
        Authentication authentication = mock(Authentication.class);
        when(repository.findByIdAndWarehouse_IdAndDeletedAtIsNull(locationId, warehouseId))
                .thenReturn(Optional.empty());
        WarehouseLocationService service = new WarehouseLocationServiceImpl(repository, warehouses,
                mock(JdbcTemplate.class));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.get(authentication, warehouseId, locationId));
        assertEquals("LOCATION_NOT_FOUND", error.getCode());
    }

    @Test
    void doesNotListLocationsWhenWarehouseAccessIsDenied() {
        UUID warehouseId = UUID.randomUUID();
        WarehouseService warehouses = mock(WarehouseService.class);
        Authentication authentication = mock(Authentication.class);
        org.mockito.Mockito.doThrow(new BusinessException(org.springframework.http.HttpStatus.FORBIDDEN,
                "WAREHOUSE_FORBIDDEN", "Denied."))
                .when(warehouses).requireAccess(authentication, warehouseId);
        WarehouseLocationService service = new WarehouseLocationServiceImpl(
                mock(WarehouseLocationRepository.class), warehouses, mock(JdbcTemplate.class));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.list(authentication, warehouseId, 0, 20, "code"));
        assertEquals("WAREHOUSE_FORBIDDEN", error.getCode());
    }
}
