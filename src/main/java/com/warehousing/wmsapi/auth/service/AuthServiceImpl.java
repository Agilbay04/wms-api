package com.warehousing.wmsapi.auth.service;

import com.warehousing.wmsapi.auth.security.JwtService;
import com.warehousing.wmsapi.auth.dto.LoginRequest;
import com.warehousing.wmsapi.auth.dto.RefreshTokenRequest;
import com.warehousing.wmsapi.auth.dto.LoginResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final AuthenticationProvider authenticationProvider;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    public AuthServiceImpl(AuthenticationProvider authenticationProvider, JwtService jwtService, RefreshTokenService refreshTokenService) { this.authenticationProvider = authenticationProvider; this.jwtService = jwtService; this.refreshTokenService = refreshTokenService; }
    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationProvider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
            return tokensFor(request.email());
        } catch (BadCredentialsException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
        }
    }
    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        try { return tokensFor(refreshTokenService.rotate(request.refreshToken())); }
        catch (IllegalArgumentException exception) { throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", exception.getMessage()); }
    }
    @Override
    public void logout(RefreshTokenRequest request) { refreshTokenService.revoke(request.refreshToken()); }
    private LoginResponse tokensFor(String email) { return new LoginResponse(jwtService.createAccessToken(email), refreshTokenService.issue(email), "Bearer"); }
}
