package com.warehousing.wmsapi.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    private static final Pattern CAMEL_CASE_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");

    @Bean
    OpenApiCustomizer snakeCaseSchemaProperties() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().values().forEach(OpenApiConfig::convertSchemaProperties);
        };
    }

    @SuppressWarnings("rawtypes")
    private static void convertSchemaProperties(Schema<?> schema) {
        if (schema == null) {
            return;
        }

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null && !properties.isEmpty()) {
            Map<String, Schema> snakeCaseProperties = new LinkedHashMap<>();
            properties.forEach((name, property) -> {
                convertSchemaProperties(property);
                snakeCaseProperties.put(toSnakeCase(name), property);
            });
            schema.setProperties(snakeCaseProperties);
        }

        if (schema.getRequired() != null) {
            schema.setRequired(schema.getRequired().stream().map(OpenApiConfig::toSnakeCase).toList());
        }
        convertSchemaProperties(schema.getItems());
        if (schema.getAdditionalProperties() instanceof Schema<?> additionalProperties) {
            convertSchemaProperties(additionalProperties);
        }
        convertSchemas(schema.getAllOf());
        convertSchemas(schema.getAnyOf());
        convertSchemas(schema.getOneOf());
    }

    @SuppressWarnings("rawtypes")
    private static void convertSchemas(List<Schema> schemas) {
        if (schemas != null) {
            schemas.forEach(OpenApiConfig::convertSchemaProperties);
        }
    }

    private static String toSnakeCase(String name) {
        return CAMEL_CASE_BOUNDARY.matcher(name).replaceAll("$1_$2").toLowerCase(Locale.ROOT);
    }
}
