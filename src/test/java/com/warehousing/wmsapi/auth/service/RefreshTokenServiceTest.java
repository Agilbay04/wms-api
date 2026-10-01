package com.warehousing.wmsapi.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.warehousing.wmsapi.config.AppProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RefreshTokenServiceTest {
    @Test
    void shouldIssueRotateAndRevokeRefreshToken() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration ttl = Duration.ofDays(7);
        AppProperties properties = new AppProperties(
                new AppProperties.JwtProperties("01234567890123456789012345678901", Duration.ofMinutes(15), ttl),
                "exports", new AppProperties.SeedAdminProperties("", "", ""));
        RefreshTokenService service = new RefreshTokenServiceImpl(redisTemplate, properties);

        String token = service.issue("staff@example.com");
        verify(valueOperations).set(startsWith("wms:auth:refresh:"), eq("staff@example.com"), eq(ttl));
        when(valueOperations.getAndDelete("wms:auth:refresh:" + token)).thenReturn("staff@example.com");
        assertEquals("staff@example.com", service.rotate(token));
        service.revoke(token);
        verify(redisTemplate).delete("wms:auth:refresh:" + token);
    }
}
