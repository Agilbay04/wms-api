package com.warehousing.wmsapi.auth.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.warehousing.wmsapi.auth.service.AccessTokenRevocationService;
import com.warehousing.wmsapi.config.AppProperties;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

class JwtAuthenticationFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doesNotAuthenticateRevokedAccessToken() throws Exception {
        AppProperties properties = new AppProperties(
                new AppProperties.JwtProperties("01234567890123456789012345678901",
                        Duration.ofMinutes(15), Duration.ofDays(7)),
                "exports", new AppProperties.SeedAdminProperties("", "", ""));
        JwtService jwtService = new JwtService(properties);
        String accessToken = jwtService.createAccessToken("staff@example.com");
        String tokenId = jwtService.parseClaims(accessToken).getId();
        AccessTokenRevocationService revocationService = mock(AccessTokenRevocationService.class);
        when(revocationService.isRevoked(tokenId)).thenReturn(true);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, revocationService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> { });

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername("staff@example.com");
    }
}
