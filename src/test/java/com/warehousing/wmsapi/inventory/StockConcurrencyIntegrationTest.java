package com.warehousing.wmsapi.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.persistence.IntegrationTestDatabase;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EnabledIf("isTestDatabaseConfigured")
class StockConcurrencyIntegrationTest {
    private static final String SCHEMA = IntegrationTestDatabase.newSchema("wms_stock_it_");

    @Autowired private StockBalanceService stockBalanceService;
    @Autowired private StockMovementService stockMovementService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    private UUID userId;
    private UUID warehouseId;
    private UUID locationId;
    private UUID productId;

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
        userId = jdbcTemplate.queryForObject("INSERT INTO users(email, name, password) VALUES (?, ?, 'test') RETURNING id",
                UUID.class, "stock-test-" + suffix + "@integration.test", "Stock tester");
        warehouseId = jdbcTemplate.queryForObject("INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "STOCK-WH-" + suffix, "Stock test warehouse");
        locationId = jdbcTemplate.queryForObject("""
                INSERT INTO warehouse_locations(warehouse_id, code, name) VALUES (?, ?, ?) RETURNING id
                """, UUID.class, warehouseId, "STOCK-LOC-" + suffix, "Stock test location");
        UUID categoryId = jdbcTemplate.queryForObject("INSERT INTO product_categories(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "STOCK-CAT-" + suffix, "Stock test category");
        productId = jdbcTemplate.queryForObject("""
                INSERT INTO products(product_category_id, sku, name, unit) VALUES (?, ?, ?, 'pcs') RETURNING id
                """, UUID.class, categoryId, "STOCK-SKU-" + suffix, "Stock test product");
        jdbcTemplate.update("""
                INSERT INTO warehouse_location_items(warehouse_id, warehouse_location_id, product_id, quantity)
                VALUES (?, ?, ?, 10)
                """, warehouseId, locationId, productId);
    }

    @Test
    void onlyOneConcurrentDeductionCanSpendTheSameAvailableStock() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> deductOnce(ready, start));
            Future<String> second = executor.submit(() -> deductOnce(ready, start));
            ready.await();
            start.countDown();

            String firstResult = first.get();
            String secondResult = second.get();
            assertEquals(1, ("SUCCESS".equals(firstResult) ? 1 : 0) + ("SUCCESS".equals(secondResult) ? 1 : 0));
            assertEquals(1, ("INSUFFICIENT_STOCK".equals(firstResult) ? 1 : 0)
                    + ("INSUFFICIENT_STOCK".equals(secondResult) ? 1 : 0));
            assertEquals(3, jdbcTemplate.queryForObject("""
                    SELECT quantity FROM warehouse_location_items
                    WHERE warehouse_location_id = ? AND product_id = ?
                    """, Integer.class, locationId, productId));
            assertEquals(1, jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM stock_movements
                    WHERE warehouse_id = ? AND product_id = ? AND movement_type = 'OUTBOUND'
                      AND movement_direction = 'OUT' AND quantity = 7 AND stock_after = 3
                    """, Integer.class, warehouseId, productId));
        } finally {
            executor.shutdownNow();
        }
    }

    private String deductOnce(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        try {
            transaction.execute(status -> {
                int stockAfter = stockBalanceService.deductStock(locationId, productId, 7);
                stockMovementService.recordOutbound(warehouseId, locationId, productId, 7, stockAfter,
                        UUID.randomUUID(), UUID.randomUUID(), userId);
                return null;
            });
            return "SUCCESS";
        } catch (BusinessException exception) {
            return exception.getCode();
        }
    }
}
