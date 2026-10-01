package com.warehousing.wmsapi.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, String tokenType) {
}
