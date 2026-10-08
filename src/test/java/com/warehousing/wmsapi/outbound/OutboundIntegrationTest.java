package com.warehousing.wmsapi.outbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.outbound.dto.OutboundCreateRequest;
import com.warehousing.wmsapi.outbound.enums.OutboundStatus;
import com.warehousing.wmsapi.outbound.dto.OutboundItemRequest;
import com.warehousing.wmsapi.outbound.dto.OutboundRejectRequest;
import com.warehousing.wmsapi.outbound.dto.OutboundResponse;
import com.warehousing.wmsapi.outbound.dto.OutboundUpdateRequest;
import com.warehousing.wmsapi.outbound.service.OutboundService;
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
class OutboundIntegrationTest {
    private static final String SCHEMA = IntegrationTestDatabase.newSchema("wms_outbound_it_");

    @Autowired private OutboundService outboundService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID warehouseId;
    private UUID foreignWarehouseId;
    private UUID locationId;
    private UUID foreignLocationId;
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
        UUID creatorId = insertUser("outbound-staff-" + suffix + "@integration.test", "Outbound staff");
        reviewerId = insertUser("outbound-reviewer-" + suffix + "@integration.test", "Outbound reviewer");
        creator = authentication("outbound-staff-" + suffix + "@integration.test");
        reviewer = authentication("outbound-reviewer-" + suffix + "@integration.test");
        warehouseId = insertWarehouse("OUTBOUND-WH-" + suffix);
        foreignWarehouseId = insertWarehouse("OUTBOUND-FOREIGN-WH-" + suffix);
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", creatorId, warehouseId);
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", reviewerId, warehouseId);
        locationId = insertLocation(warehouseId, "OUTBOUND-LOC-" + suffix);
        foreignLocationId = insertLocation(foreignWarehouseId, "OUTBOUND-FOREIGN-LOC-" + suffix);
        UUID categoryId = jdbcTemplate.queryForObject("INSERT INTO product_categories(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "OUTBOUND-CAT-" + suffix, "Outbound category");
        firstProductId = insertProduct(categoryId, "OUTBOUND-SKU-A-" + suffix);
        secondProductId = insertProduct(categoryId, "OUTBOUND-SKU-B-" + suffix);
        insertBalance(firstProductId, 10);
        insertBalance(secondProductId, 2);
    }

    @Test
    void approvalDeductsStockWritesMovementAndMakesOutboundImmutable() {
        OutboundResponse created = createOutbound(List.of(item(firstProductId, 4), item(secondProductId, 1)));
        outboundService.submit(creator, created.id());
        assertEquals(10, stock(firstProductId));
        assertEquals(2, stock(secondProductId));

        OutboundResponse approved = outboundService.approve(reviewer, created.id());
        assertEquals(OutboundStatus.APPROVED, approved.status());
        assertEquals(6, stock(firstProductId));
        assertEquals(1, stock(secondProductId));
        assertEquals(2, count("SELECT count(*) FROM stock_movements WHERE source_entity_id = ? AND movement_type = 'OUTBOUND' AND movement_direction = 'OUT'", created.id()));
        assertEquals(1, count("SELECT count(*) FROM stock_movements WHERE source_entity_id = ? AND quantity = 4 AND stock_after = 6", created.id()));
        assertEquals(1, count("SELECT count(*) FROM audit_trails WHERE entity_id = ? AND action = 'APPROVE' AND user_id = ?", created.id(), reviewerId));
        assertEquals(reviewerId, jdbcTemplate.queryForObject(
                "SELECT reviewed_by_user_id FROM outbounds WHERE id = ?", UUID.class, created.id()));

        BusinessException immutable = assertThrows(BusinessException.class, () -> outboundService.update(creator,
                created.id(), new OutboundUpdateRequest("Changed after approval", List.of(item(firstProductId, 1)))));
        assertEquals("OUTBOUND_NOT_EDITABLE", immutable.getCode());
    }

    @Test
    void insufficientStockRollsBackEveryBalanceMovementAndApproval() {
        OutboundResponse created = createOutbound(List.of(item(firstProductId, 4), item(secondProductId, 3)));
        outboundService.submit(creator, created.id());

        BusinessException insufficient = assertThrows(BusinessException.class,
                () -> outboundService.approve(reviewer, created.id()));
        assertEquals("INSUFFICIENT_STOCK", insufficient.getCode());
        assertEquals(10, stock(firstProductId));
        assertEquals(2, stock(secondProductId));
        assertEquals(0, count("SELECT count(*) FROM stock_movements WHERE source_entity_id = ?", created.id()));
        assertEquals(0, count("SELECT count(*) FROM audit_trails WHERE entity_id = ? AND action = 'APPROVE'", created.id()));
        assertEquals(OutboundStatus.PENDING, outboundService.get(creator, created.id()).status());
    }

    @Test
    void rejectionCanBeRevisedAndResubmittedByCreator() {
        OutboundResponse created = createOutbound(List.of(item(firstProductId, 4)));
        outboundService.submit(creator, created.id());
        OutboundResponse rejected = outboundService.reject(reviewer, created.id(), new OutboundRejectRequest("  Recount items  "));
        assertEquals(OutboundStatus.REJECTED, rejected.status());
        assertEquals("Recount items", rejected.rejectionNote());

        OutboundResponse revised = outboundService.update(creator, created.id(),
                new OutboundUpdateRequest("Revised request", List.of(item(firstProductId, 5))));
        assertEquals(OutboundStatus.PENDING, revised.status());
        assertNull(revised.rejectionNote());
        assertNull(revised.submittedAt());
        assertEquals(5, revised.items().get(0).quantity());
        assertEquals(OutboundStatus.PENDING, outboundService.submit(creator, created.id()).status());
    }

    @Test
    void warehouseAccessAndLocationOwnershipAreEnforced() {
        BusinessException forbidden = assertThrows(BusinessException.class, () -> outboundService.create(creator,
                new OutboundCreateRequest(foreignWarehouseId, null, List.of(item(firstProductId, 1)))));
        assertEquals("WAREHOUSE_FORBIDDEN", forbidden.getCode());

        BusinessException foreignLocation = assertThrows(BusinessException.class, () -> outboundService.create(creator,
                new OutboundCreateRequest(warehouseId, null,
                        List.of(new OutboundItemRequest(firstProductId, foreignLocationId, 1, null)))));
        assertEquals("INVALID_OUTBOUND_LOCATION", foreignLocation.getCode());
    }

    private OutboundResponse createOutbound(List<OutboundItemRequest> items) {
        return outboundService.create(creator, new OutboundCreateRequest(warehouseId, "outbound test", items));
    }

    private OutboundItemRequest item(UUID productId, int quantity) {
        return new OutboundItemRequest(productId, locationId, quantity, null);
    }

    private UUID insertUser(String email, String name) {
        return jdbcTemplate.queryForObject("INSERT INTO users(email, name, password) VALUES (?, ?, 'test') RETURNING id",
                UUID.class, email, name);
    }

    private UUID insertWarehouse(String code) {
        return jdbcTemplate.queryForObject("INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, code, code);
    }

    private UUID insertLocation(UUID warehouse, String code) {
        return jdbcTemplate.queryForObject("INSERT INTO warehouse_locations(warehouse_id, code, name) VALUES (?, ?, ?) RETURNING id",
                UUID.class, warehouse, code, code);
    }

    private UUID insertProduct(UUID category, String sku) {
        return jdbcTemplate.queryForObject("INSERT INTO products(product_category_id, sku, name, unit) VALUES (?, ?, ?, 'pcs') RETURNING id",
                UUID.class, category, sku, sku);
    }

    private void insertBalance(UUID product, int quantity) {
        jdbcTemplate.update("INSERT INTO warehouse_location_items(warehouse_id, warehouse_location_id, product_id, quantity) VALUES (?, ?, ?, ?)",
                warehouseId, locationId, product, quantity);
    }

    private int stock(UUID product) {
        return jdbcTemplate.queryForObject("SELECT quantity FROM warehouse_location_items WHERE warehouse_location_id = ? AND product_id = ?",
                Integer.class, locationId, product);
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
