package com.warehousing.wmsapi.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;

class PermissionServiceTest {
    @Test
    void shouldRejectUnauthenticatedCaller() {
        assertFalse(new PermissionServiceImpl(mock(JdbcTemplate.class)).hasPermission(null, "PRODUCTS", "READ"));
    }

    @Test
    void shouldReturnDatabaseAuthorizationDecision() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("staff@example.com");
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq("staff@example.com"), eq("PRODUCTS"), eq("READ")))
                .thenReturn(true);

        assertTrue(new PermissionServiceImpl(jdbcTemplate).hasPermission(authentication, "PRODUCTS", "READ"));
    }
}
