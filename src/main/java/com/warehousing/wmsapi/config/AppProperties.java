package com.warehousing.wmsapi.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        JwtProperties jwt,
        String exportDirectory,
        SeedAdminProperties seedAdmin
) {

    public record JwtProperties(
            String secret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl
    ) {
    }

    public record SeedAdminProperties(String email, String name, String password) {
    }
}
