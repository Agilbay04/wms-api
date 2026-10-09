package com.warehousing.wmsapi.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class PermissionServiceTest {
    @Test
    void shouldRejectUnauthenticatedCaller() {
        assertFalse(new PermissionServiceImpl(mock(AccessControlRepository.class)).hasPermission(null, "PRODUCTS", "READ"));
    }

    @Test
    void shouldReturnDatabaseAuthorizationDecision() {
        AccessControlRepository repository = mock(AccessControlRepository.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("staff@example.com");
        when(repository.hasPermission("staff@example.com", "PRODUCTS", "READ")).thenReturn(true);

        assertTrue(new PermissionServiceImpl(repository).hasPermission(authentication, "PRODUCTS", "READ"));
    }
}
