package com.warehousing.wmsapi.auth.service;

public interface RefreshTokenService {
    String issue(String email);
    String rotate(String token);
    void revoke(String token);
}
