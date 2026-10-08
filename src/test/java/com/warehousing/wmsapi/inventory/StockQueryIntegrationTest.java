package com.warehousing.wmsapi.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.persistence.IntegrationTestDatabase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EnabledIf("isTestDatabaseConfigured")
class StockQueryIntegrationTest {
    private static final String SCHEMA = IntegrationTestDatabase.newSchema("wms_stock_query_it_");

    @Autowired private StockQueryService stockQueryService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String userEmail;
    private UUID userId;
    private UUID accessibleWarehouseId;
    private UUID inaccessibleWarehouseId;
    private UUID accessibleProductId;

    public static boolean isTestDatabaseConfigured() {
        return IntegrationTestDatabase.isConfigured();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        IntegrationTestDatabase.configure(registry, SCHEMA);
    }

    @AfterAll
    static void cleanSchema() throws Exception {
        IntegrationTestDatabase.dropSchema(SCHEMA);
    }

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        userEmail = "stock-query-" + suffix + "@integration.test";
        userId = jdbcTemplate.queryForObject("INSERT INTO users(email, name, password) VALUES (?, ?, 'test') RETURNING id",
                UUID.class, userEmail, "Stock query tester");
        accessibleWarehouseId = insertWarehouse("VISIBLE-" + suffix);
        inaccessibleWarehouseId = insertWarehouse("HIDDEN-" + suffix);
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", userId,
                accessibleWarehouseId);
        UUID visibleLocationId = insertLocation(accessibleWarehouseId, "VISIBLE-LOC-" + suffix);
        UUID hiddenLocationId = insertLocation(inaccessibleWarehouseId, "HIDDEN-LOC-" + suffix);
        UUID categoryId = jdbcTemplate.queryForObject("INSERT INTO product_categories(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "QUERY-CAT-" + suffix, "Query category");
        accessibleProductId = insertProduct(categoryId, "VISIBLE-SKU-" + suffix);
        UUID hiddenProductId = insertProduct(categoryId, "HIDDEN-SKU-" + suffix);
        insertBalance(accessibleWarehouseId, visibleLocationId, accessibleProductId, 12);
        insertBalance(inaccessibleWarehouseId, hiddenLocationId, hiddenProductId, 77);
        insertMovement(accessibleWarehouseId, visibleLocationId, accessibleProductId, 4, 12);
        insertMovement(inaccessibleWarehouseId, hiddenLocationId, hiddenProductId, 77, 77);
    }

    @Test
    void stockAndMovementPagesIncludeOnlyWarehousesAssignedToTheUser() {
        var authentication = new UsernamePasswordAuthenticationToken(userEmail, "test", List.of());
        BasePageRequest request = BasePageRequest.of(1, 10, "created_at", "desc", null);

        var stocks = stockQueryService.stocks(authentication, request);
        assertEquals(1, stocks.totalElements());
        assertEquals(accessibleWarehouseId, stocks.content().get(0).warehouseId());
        assertEquals(12, stocks.content().get(0).quantity());

        var movements = stockQueryService.movements(authentication, request);
        assertEquals(1, movements.totalElements());
        assertEquals(accessibleWarehouseId, movements.content().get(0).warehouseId());
        assertEquals("IN", movements.content().get(0).movementDirection());
        assertEquals(4, movements.content().get(0).quantity());
        assertEquals(12, movements.content().get(0).stockAfter());

        var searched = stockQueryService.stocks(authentication,
                BasePageRequest.of(1, 10, "sku", "asc", "VISIBLE-SKU"));
        assertEquals(1, searched.totalElements());
        assertEquals(accessibleProductId, searched.content().get(0).productId());
    }

    private UUID insertWarehouse(String code) {
        return jdbcTemplate.queryForObject("INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id", UUID.class,
                code, code);
    }

    private UUID insertLocation(UUID warehouseId, String code) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO warehouse_locations(warehouse_id, code, name) VALUES (?, ?, ?) RETURNING id
                """, UUID.class, warehouseId, code, code);
    }

    private UUID insertProduct(UUID categoryId, String sku) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO products(product_category_id, sku, name, unit) VALUES (?, ?, ?, 'pcs') RETURNING id
                """, UUID.class, categoryId, sku, sku);
    }

    private void insertBalance(UUID warehouseId, UUID locationId, UUID productId, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO warehouse_location_items(warehouse_id, warehouse_location_id, product_id, quantity)
                VALUES (?, ?, ?, ?)
                """, warehouseId, locationId, productId, quantity);
    }

    private void insertMovement(UUID warehouseId, UUID locationId, UUID productId, int quantity, int stockAfter) {
        jdbcTemplate.update("""
                INSERT INTO stock_movements(warehouse_id, warehouse_location_id, product_id, movement_type,
                    movement_direction, quantity, stock_after, source_entity_type, source_entity_id,
                    created_by_user_id)
                VALUES (?, ?, ?, 'INBOUND', 'IN', ?, ?, 'INBOUND', ?, ?)
                """, warehouseId, locationId, productId, quantity, stockAfter, UUID.randomUUID(), userId);
    }
}
