package com.warehousing.wmsapi.common.pagination;

import com.warehousing.wmsapi.category.controller.ProductCategoryController;
import com.warehousing.wmsapi.location.controller.WarehouseLocationController;
import com.warehousing.wmsapi.product.controller.ProductController;
import com.warehousing.wmsapi.warehouse.controller.WarehouseController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MasterListDefaultsTest {

    @Test
    void allMasterListsDefaultToFirstPageAndTenItems() {
        for (Class<?> controller : new Class<?>[] {
                ProductCategoryController.class,
                ProductController.class,
                WarehouseController.class,
                WarehouseLocationController.class
        }) {
            Method listMethod = java.util.Arrays.stream(controller.getDeclaredMethods())
                    .filter(method -> method.getName().equals("list"))
                    .findFirst().orElseThrow();
            String[] defaults = java.util.Arrays.stream(listMethod.getParameters())
                    .map(parameter -> parameter.getAnnotations())
                    .flatMap(java.util.Arrays::stream)
                    .filter(annotation -> annotation instanceof RequestParam)
                    .map(annotation -> ((RequestParam) annotation).defaultValue())
                    .toArray(String[]::new);

            assertEquals("1", defaults[0], controller.getSimpleName());
            assertEquals("10", defaults[
                1], controller.getSimpleName());
        }
    }
}
