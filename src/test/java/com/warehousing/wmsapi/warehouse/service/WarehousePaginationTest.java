package com.warehousing.wmsapi.warehouse.service;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.security.WarehouseAccessService;
import com.warehousing.wmsapi.warehouse.repository.WarehouseRepository;
import com.warehousing.wmsapi.warehouse.repository.WarehouseQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WarehousePaginationTest {

    @Test
    void convertsPublicPagesToSqlOffsets() {
        WarehouseQueryRepository queryRepository = mock(WarehouseQueryRepository.class);
        when(queryRepository.countVisible("admin@example.com", null)).thenReturn(21L);
        WarehouseService service = new WarehouseServiceImpl(mock(WarehouseRepository.class),
                queryRepository, mock(WarehouseAccessService.class));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin@example.com");

        var first = service.list(authentication, 1, 10, "code");
        verify(queryRepository).findVisible("admin@example.com", null, "code", "DESC", 10, 0L);
        assertEquals(1, first.page());
        assertEquals(3, first.totalPages());

        var second = service.list(authentication, 2, 10, "code");
        verify(queryRepository).findVisible("admin@example.com", null, "code", "DESC", 10, 10L);
        assertEquals(2, second.page());
    }

    @Test
    void rejectsPageZero() {
        WarehouseService service = new WarehouseServiceImpl(mock(WarehouseRepository.class),
                mock(WarehouseQueryRepository.class), mock(WarehouseAccessService.class));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.list(mock(Authentication.class), 0, 10, "code"));
        assertEquals("INVALID_PAGE_REQUEST", error.getCode());
    }
}
