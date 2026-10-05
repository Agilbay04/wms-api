package com.warehousing.wmsapi.auth.security;

import java.time.Instant;

public record JwtAuthenticationDetails(String tokenId, Instant expiresAt) {
}
