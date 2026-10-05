package com.warehousing.wmsapi.auth.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenRevocationService {
    private static final String KEY_PREFIX = "wms:auth:revoked-access:";

    private final StringRedisTemplate redisTemplate;

    public AccessTokenRevocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revoke(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "revoked", ttl);
        }
    }

    public boolean isRevoked(String tokenId) {
        return tokenId != null && Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
    }
}
