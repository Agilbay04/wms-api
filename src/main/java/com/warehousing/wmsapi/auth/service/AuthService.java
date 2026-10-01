package com.warehousing.wmsapi.auth.service;

import com.warehousing.wmsapi.auth.dto.LoginRequest;
import com.warehousing.wmsapi.auth.dto.LoginResponse;
import com.warehousing.wmsapi.auth.dto.RefreshTokenRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse refresh(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
}
