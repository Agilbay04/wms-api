package com.warehousing.wmsapi.common.pagination;

import com.warehousing.wmsapi.category.controller.ProductCategoryController;
import com.warehousing.wmsapi.location.controller.WarehouseLocationController;
import com.warehousing.wmsapi.product.controller.ProductController;
import com.warehousing.wmsapi.warehouse.controller.WarehouseController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ModelAttribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MasterListDefaultsTest {

    @Test
    void allMasterListsUseOptionalSharedRequestWithPaginationDefaults() {
        for (Class<?> controller : new Class<?>[] {
                ProductCategoryController.class,
                ProductController.class,
                WarehouseController.class,
                WarehouseLocationController.class
        }) {
            Method listMethod = java.util.Arrays.stream(controller.getDeclaredMethods())
                    .filter(method -> method.getName().equals("list"))
                    .findFirst().orElseThrow();
            var requestParameter = java.util.Arrays.stream(listMethod.getParameters())
                    .filter(parameter -> parameter.getType() == BasePageRequest.class)
                    .findFirst().orElseThrow();
            assertNotNull(requestParameter.getAnnotation(ModelAttribute.class), controller.getSimpleName());
        }

        BasePageRequest request = new BasePageRequest();
        assertEquals(1, request.getPage());
        assertEquals(10, request.getSize());
        assertEquals("created_at", request.getSort());
        assertEquals("desc", request.getOrder());
        assertNull(request.getSearch());
    }
}
