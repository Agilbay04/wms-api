package com.warehousing.wmsapi.warehouse.service;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.security.WarehouseAccessService;
import com.warehousing.wmsapi.warehouse.repository.WarehouseRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarehousePaginationTest {

    @Test
    void convertsPublicPagesToSqlOffsets() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        WarehouseService service = new WarehouseServiceImpl(mock(WarehouseRepository.class),
                mock(WarehouseAccessService.class), jdbcTemplate);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin@example.com");

        var first = service.list(authentication, 1, 10, "code");
        assertEquals(0L, jdbcTemplate.lastOffset);
        assertEquals(1, first.page());
        assertEquals(3, first.totalPages());

        var second = service.list(authentication, 2, 10, "code");
        assertEquals(10L, jdbcTemplate.lastOffset);
        assertEquals(2, second.page());
    }

    @Test
    void rejectsPageZero() {
        WarehouseService service = new WarehouseServiceImpl(mock(WarehouseRepository.class),
                mock(WarehouseAccessService.class), mock(JdbcTemplate.class));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.list(mock(Authentication.class), 0, 10, "code"));
        assertEquals("INVALID_PAGE_REQUEST", error.getCode());
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private long lastOffset;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            return (T) Long.valueOf(21);
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            lastOffset = (long) args[2];
            return List.of();
        }
    }
}
