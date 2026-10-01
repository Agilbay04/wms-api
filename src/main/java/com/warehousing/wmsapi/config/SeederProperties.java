package com.warehousing.wmsapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seeder")
public record SeederProperties(boolean enableDbSetup, String resultDirectory) {
}
