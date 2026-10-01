package com.warehousing.wmsapi.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.warehousing.wmsapi.config.AppProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void shouldCreateTokenWhoseSubjectCanBeRead() {
        AppProperties properties = new AppProperties(
                new AppProperties.JwtProperties("01234567890123456789012345678901", Duration.ofMinutes(15), Duration.ofDays(7)),
                "exports",
                new AppProperties.SeedAdminProperties("admin@example.com", "Admin", "password")
        );
        JwtService jwtService = new JwtService(properties);

        String token = jwtService.createAccessToken("admin@example.com");

        assertEquals("admin@example.com", jwtService.getSubject(token));
    }
}
