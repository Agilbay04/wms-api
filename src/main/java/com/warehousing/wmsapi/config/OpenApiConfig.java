package com.warehousing.wmsapi.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Warehouse Management API", version = "1.0.0",
        description = """
                API for warehouse operations, stock, approvals, reporting, and exports.

                Request and response JSON property names use snake_case. Successful responses use the common
                envelope `{success, code, message, data, errors}`. Errors use the same envelope with `success=false`,
                `data=null`, and field errors keyed by snake_case property names. Bean validation failures return 422.

                Paginated list endpoints accept optional `page` (default 1), `size` (default 10, maximum 100),
                `sort` (endpoint-supported field), `order` (`asc` or `desc`, default `desc`), and `search`.
                Authentication uses a bearer JWT. Inbound, outbound, transfer, and adjustment records follow
                draft -> submit -> approve/reject workflows; approval and rejection require their corresponding
                permission, and creators cannot review their own records. Workflow conflicts return 409 with a
                stable error code such as `*_NOT_REVIEWABLE`, `*_NOT_EDITABLE`, or `INSUFFICIENT_STOCK`; a creator
                attempting self-review receives 403 with a `*_SELF_REVIEW_FORBIDDEN` code.
                """))
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

    @Bean
    OpenApiCustomizer commonErrorResponses() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new io.swagger.v3.oas.models.Components());
            }
            openApi.getComponents().addSchemas("ApiErrorEnvelope", errorEnvelopeSchema());
            if (openApi.getPaths() == null) return;
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
                    addCommonErrorResponses(operation)));
        };
    }

    private static void addCommonErrorResponses(Operation operation) {
        if (operation.getResponses() == null) operation.setResponses(new ApiResponses());
        addErrorResponse(operation, "400", "Malformed request or invalid query parameter.");
        addErrorResponse(operation, "401", "Authentication is required or the supplied token is invalid.");
        addErrorResponse(operation, "403", "The caller lacks a required permission or warehouse access.");
        addErrorResponse(operation, "404", "The requested resource was not found or is not visible to the caller.");
        addErrorResponse(operation, "409", "The request conflicts with current resource state or existing data.");
        addErrorResponse(operation, "422", "Request validation or workflow rules failed.");
        addErrorResponse(operation, "500", "An unexpected server error occurred.");
    }

    private static void addErrorResponse(Operation operation, String status, String description) {
        operation.getResponses().putIfAbsent(status,
                new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description(description)
                        .content(new Content().addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiErrorEnvelope")))));
    }

    private static Schema<?> errorEnvelopeSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("success", new Schema<Boolean>().type("boolean")._default(false));
        schema.addProperty("code", new IntegerSchema().format("int32"));
        schema.addProperty("message", new StringSchema());
        schema.addProperty("data", new Schema<>().nullable(true));
        schema.addProperty("errors", new ObjectSchema().additionalProperties(new StringSchema()));
        schema.setRequired(List.of("success", "code", "message", "data", "errors"));
        return schema;
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
