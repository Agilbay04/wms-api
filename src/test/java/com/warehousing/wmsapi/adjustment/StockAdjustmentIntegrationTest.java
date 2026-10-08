package com.warehousing.wmsapi.adjustment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentCreateRequest;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentItemRequest;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentRejectRequest;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentResponse;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentUpdateRequest;
import com.warehousing.wmsapi.adjustment.enums.StockAdjustmentStatus;
import com.warehousing.wmsapi.adjustment.enums.StockAdjustmentType;
import com.warehousing.wmsapi.adjustment.service.StockAdjustmentService;
import com.warehousing.wmsapi.common.error.BusinessException;
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
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EnabledIf("isTestDatabaseConfigured")
class StockAdjustmentIntegrationTest {
    private static final String SCHEMA = IntegrationTestDatabase.newSchema("wms_adjustment_it_");

    @Autowired private StockAdjustmentService adjustmentService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID warehouseId;
    private UUID locationId;
    private UUID firstProductId;
    private UUID secondProductId;
    private UUID reviewerId;
    private Authentication creator;
    private Authentication reviewer;

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
        UUID creatorId = insertUser("adjustment-staff-" + suffix + "@integration.test", "Adjustment staff");
        reviewerId = insertUser("adjustment-reviewer-" + suffix + "@integration.test", "Adjustment reviewer");
        creator = authentication("adjustment-staff-" + suffix + "@integration.test");
        reviewer = authentication("adjustment-reviewer-" + suffix + "@integration.test");
        warehouseId = jdbcTemplate.queryForObject("INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "ADJ-WH-" + suffix, "Adjustment warehouse");
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", creatorId, warehouseId);
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", reviewerId, warehouseId);
        locationId = jdbcTemplate.queryForObject("""
                INSERT INTO warehouse_locations(warehouse_id, code, name) VALUES (?, ?, ?) RETURNING id
                """, UUID.class, warehouseId, "ADJ-LOC-" + suffix, "Adjustment location");
        UUID categoryId = jdbcTemplate.queryForObject("INSERT INTO product_categories(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "ADJ-CAT-" + suffix, "Adjustment category");
        firstProductId = insertProduct(categoryId, "ADJ-SKU-A-" + suffix);
        secondProductId = insertProduct(categoryId, "ADJ-SKU-B-" + suffix);
        insertBalance(firstProductId, 10);
        insertBalance(secondProductId, 8);
    }

    @Test
    void positiveAndNegativeChangesCreateDirectionalMovementsAndReviewerAudit() {
        StockAdjustmentResponse created = createAdjustment(StockAdjustmentType.STOCK_OPNAME, "Counted stock on shelf",
                List.of(item(firstProductId, 5), item(secondProductId, -3)));
        adjustmentService.submit(creator, created.id());
        assertEquals(10, stock(firstProductId));
        assertEquals(8, stock(secondProductId));
        assertEquals(0, count("SELECT count(*) FROM stock_movements WHERE source_entity_id = ?", created.id()));

        StockAdjustmentResponse approved = adjustmentService.approve(reviewer, created.id());
        assertEquals(StockAdjustmentStatus.APPROVED, approved.status());
        assertEquals(15, stock(firstProductId));
        assertEquals(5, stock(secondProductId));
        assertEquals(1, count("""
                SELECT count(*) FROM stock_movements WHERE source_entity_id = ? AND movement_type = 'STOCK_ADJUSTMENT'
                    AND movement_direction = 'IN' AND quantity = 5 AND stock_after = 15
                """, created.id()));
        assertEquals(1, count("""
                SELECT count(*) FROM stock_movements WHERE source_entity_id = ? AND movement_type = 'STOCK_ADJUSTMENT'
                    AND movement_direction = 'OUT' AND quantity = 3 AND stock_after = 5
                """, created.id()));
        assertEquals(1, count("""
                SELECT count(*) FROM audit_trails WHERE entity_id = ? AND action = 'APPROVE' AND user_id = ?
                    AND description ILIKE '%Counted stock on shelf%'
                """, created.id(), reviewerId));
        assertEquals(reviewerId, jdbcTemplate.queryForObject(
                "SELECT reviewed_by_user_id FROM stock_adjustments WHERE id = ?", UUID.class, created.id()));
    }

    @Test
    void anyNegativeResultRejectsWholeApprovalWithoutMovement() {
        StockAdjustmentResponse created = createAdjustment(StockAdjustmentType.CORRECTION, "Correct counted stock",
                List.of(item(firstProductId, 4), item(secondProductId, -9)));
        adjustmentService.submit(creator, created.id());

        BusinessException negative = org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
                () -> adjustmentService.approve(reviewer, created.id()));
        assertEquals("NEGATIVE_STOCK_ADJUSTMENT", negative.getCode());
        assertEquals(10, stock(firstProductId));
        assertEquals(8, stock(secondProductId));
        assertEquals(0, count("SELECT count(*) FROM stock_movements WHERE source_entity_id = ?", created.id()));
        assertEquals(0, count("SELECT count(*) FROM audit_trails WHERE entity_id = ? AND action = 'APPROVE'",
                created.id()));
        assertEquals(StockAdjustmentStatus.PENDING, adjustmentService.get(creator, created.id()).status());
    }

    @Test
    void missingReasonIsRejectedAndRejectedDraftCanBeRevised() {
        BusinessException noReason = org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
                () -> createAdjustment(StockAdjustmentType.DAMAGED_GOODS, "  ", List.of(item(firstProductId, -1))));
        assertEquals("ADJUSTMENT_REASON_REQUIRED", noReason.getCode());

        StockAdjustmentResponse created = createAdjustment(StockAdjustmentType.LOST_GOODS, "Misplaced item",
                List.of(item(firstProductId, -1)));
        adjustmentService.submit(creator, created.id());
        StockAdjustmentResponse rejected = adjustmentService.reject(reviewer, created.id(),
                new StockAdjustmentRejectRequest("Need recount"));
        assertEquals(StockAdjustmentStatus.REJECTED, rejected.status());
        StockAdjustmentResponse revised = adjustmentService.update(creator, created.id(),
                new StockAdjustmentUpdateRequest(StockAdjustmentType.CORRECTION, "Verified count",
                        List.of(item(firstProductId, 2))));
        assertEquals(StockAdjustmentStatus.PENDING, revised.status());
        assertEquals("Verified count", revised.reason());
        assertNull(revised.rejectionNote());
        assertNull(revised.submittedAt());
        assertEquals(2, revised.items().get(0).quantityChange());
    }

    private StockAdjustmentResponse createAdjustment(StockAdjustmentType type, String reason,
            List<StockAdjustmentItemRequest> items) {
        return adjustmentService.create(creator, new StockAdjustmentCreateRequest(warehouseId, type, reason, items));
    }

    private StockAdjustmentItemRequest item(UUID productId, int change) {
        return new StockAdjustmentItemRequest(productId, locationId, change, null);
    }

    private UUID insertUser(String email, String name) {
        return jdbcTemplate.queryForObject("INSERT INTO users(email, name, password) VALUES (?, ?, 'test') RETURNING id",
                UUID.class, email, name);
    }

    private UUID insertProduct(UUID category, String sku) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO products(product_category_id, sku, name, unit) VALUES (?, ?, ?, 'pcs') RETURNING id
                """, UUID.class, category, sku, sku);
    }

    private void insertBalance(UUID productId, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO warehouse_location_items(warehouse_id, warehouse_location_id, product_id, quantity)
                VALUES (?, ?, ?, ?)
                """, warehouseId, locationId, productId, quantity);
    }

    private int stock(UUID productId) {
        return jdbcTemplate.queryForObject("""
                SELECT quantity FROM warehouse_location_items WHERE warehouse_location_id = ? AND product_id = ?
                """, Integer.class, locationId, productId);
    }

    private int count(String sql, UUID id) {
        return jdbcTemplate.queryForObject(sql, Integer.class, id);
    }

    private int count(String sql, UUID id, UUID userId) {
        return jdbcTemplate.queryForObject(sql, Integer.class, id, userId);
    }

    private static Authentication authentication(String email) {
        return new UsernamePasswordAuthenticationToken(email, "integration-test", List.of());
    }
}
