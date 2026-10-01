package com.warehousing.wmsapi.config;

import com.warehousing.wmsapi.auth.controller.AuthController;
import com.warehousing.wmsapi.category.controller.ProductCategoryController;
import com.warehousing.wmsapi.location.controller.WarehouseLocationController;
import com.warehousing.wmsapi.product.controller.ProductController;
import com.warehousing.wmsapi.warehouse.controller.WarehouseController;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenApiSecurityTest {

    @Test
    void definesHttpBearerJwtScheme() {
        SecurityScheme scheme = OpenApiConfig.class.getAnnotation(SecurityScheme.class);

        assertNotNull(scheme);
        assertEquals("bearerAuth", scheme.name());
        assertEquals(SecuritySchemeType.HTTP, scheme.type());
        assertEquals("bearer", scheme.scheme());
        assertEquals("JWT", scheme.bearerFormat());
    }

    @Test
    void requiresBearerTokenForMasterDataButNotAuthEndpoints() {
        for (Class<?> controller : new Class<?>[] {
                ProductCategoryController.class,
                ProductController.class,
                WarehouseController.class,
                WarehouseLocationController.class
        }) {
            SecurityRequirement requirement = controller.getAnnotation(SecurityRequirement.class);
            assertNotNull(requirement, controller.getSimpleName());
            assertEquals("bearerAuth", requirement.name());
        }

        assertNull(AuthController.class.getAnnotation(SecurityRequirement.class));
    }
}
