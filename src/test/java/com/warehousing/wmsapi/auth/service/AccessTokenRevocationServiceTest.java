package com.warehousing.wmsapi.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AccessTokenRevocationServiceTest {
    @Test
    void storesRevokedTokenIdUntilAccessTokenExpiry() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AccessTokenRevocationService service = new AccessTokenRevocationService(redisTemplate);

        service.revoke("jwt-id", Instant.now().plusSeconds(60));

        verify(valueOperations).set(eq("wms:auth:revoked-access:jwt-id"), eq("revoked"), any(Duration.class));
    }

    @Test
    void doesNotStoreAlreadyExpiredToken() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        AccessTokenRevocationService service = new AccessTokenRevocationService(redisTemplate);

        service.revoke("expired-jwt-id", Instant.now().minusSeconds(1));

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void checksRevokedTokenIdInRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.hasKey("wms:auth:revoked-access:jwt-id")).thenReturn(true);
        AccessTokenRevocationService service = new AccessTokenRevocationService(redisTemplate);

        assertTrue(service.isRevoked("jwt-id"));
        assertFalse(service.isRevoked(null));
    }
}
