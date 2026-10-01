package com.warehousing.wmsapi.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.warehousing.wmsapi.auth.security.JwtService;
import com.warehousing.wmsapi.auth.dto.LoginRequest;
import com.warehousing.wmsapi.auth.dto.RefreshTokenRequest;
import com.warehousing.wmsapi.auth.dto.LoginResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;

class AuthServiceTest {

    @Test
    void shouldIssueTokensAfterSuccessfulLogin() {
        AuthenticationProvider provider = mock(AuthenticationProvider.class);
        JwtService jwtService = mock(JwtService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        when(jwtService.createAccessToken("admin@example.com")).thenReturn("access-token");
        when(refreshTokenService.issue("admin@example.com")).thenReturn("refresh-token");

        LoginResponse response = new AuthServiceImpl(provider, jwtService, refreshTokenService)
                .login(new LoginRequest("admin@example.com", "password123"));

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
    }

    @Test
    void shouldMapBadCredentialsToUnauthorized() {
        AuthenticationProvider provider = mock(AuthenticationProvider.class);
        when(provider.authenticate(any())).thenThrow(new BadCredentialsException("invalid"));
        AuthService service = new AuthServiceImpl(provider, mock(JwtService.class), mock(RefreshTokenService.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.login(new LoginRequest("admin@example.com", "password123")));

        assertEquals("INVALID_CREDENTIALS", exception.getCode());
        assertEquals(401, exception.getStatus().value());
    }

    @Test
    void shouldMapInvalidRefreshTokenToUnauthorized() {
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        when(refreshTokenService.rotate("invalid")).thenThrow(new IllegalArgumentException("invalid token"));
        AuthService service = new AuthServiceImpl(mock(AuthenticationProvider.class), mock(JwtService.class), refreshTokenService);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.refresh(new RefreshTokenRequest("invalid")));

        assertEquals("INVALID_REFRESH_TOKEN", exception.getCode());
        assertEquals(401, exception.getStatus().value());
    }
}
