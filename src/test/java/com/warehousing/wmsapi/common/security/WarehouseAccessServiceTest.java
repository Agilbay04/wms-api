package com.warehousing.wmsapi.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;

class WarehouseAccessServiceTest {
    @Test
    void shouldReturnFalseWhenDatabaseDeniesWarehouseAccess() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("staff@example.com");
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq("staff@example.com"), any(UUID.class)))
                .thenReturn(false);

        assertFalse(new WarehouseAccessServiceImpl(jdbcTemplate).canAccessWarehouse(authentication, UUID.randomUUID()));
    }
}
