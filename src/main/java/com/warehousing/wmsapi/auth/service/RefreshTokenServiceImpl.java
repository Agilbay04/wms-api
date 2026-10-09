package com.warehousing.wmsapi.auth.service;

import com.warehousing.wmsapi.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private static final String KEY_PREFIX = "wms:auth:refresh:";
    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();
        @Override
    public String issue(String email) {
        byte[] bytes = new byte[32]; secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redisTemplate.opsForValue().set(redisKey(token), email, appProperties.jwt().refreshTokenTtl());
        return token;
    }
    @Override
    public String rotate(String token) {
        String email = redisTemplate.opsForValue().getAndDelete(redisKey(token));
        if (email == null) throw new IllegalArgumentException("Refresh token is invalid or expired.");
        return email;
    }
    @Override
    public void revoke(String token) { redisTemplate.delete(redisKey(token)); }

    private String redisKey(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
