package com.warehousing.wmsapi.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.persistence.IntegrationTestDatabase;
import com.warehousing.wmsapi.transfer.dto.StockTransferCreateRequest;
import com.warehousing.wmsapi.transfer.enums.StockTransferStatus;
import com.warehousing.wmsapi.transfer.dto.StockTransferItemRequest;
import com.warehousing.wmsapi.transfer.dto.StockTransferResponse;
import com.warehousing.wmsapi.transfer.dto.StockTransferRejectRequest;
import com.warehousing.wmsapi.transfer.dto.StockTransferUpdateRequest;
import com.warehousing.wmsapi.transfer.service.StockTransferService;
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
class StockTransferIntegrationTest {
    private static final String SCHEMA = IntegrationTestDatabase.newSchema("wms_transfer_it_");

    @Autowired private StockTransferService transferService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID warehouseId;
    private UUID foreignWarehouseId;
    private UUID sourceLocationId;
    private UUID destinationLocationId;
    private UUID foreignLocationId;
    private UUID firstProductId;
    private UUID secondProductId;
    private UUID creatorId;
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
        creatorId = insertUser("transfer-staff-" + suffix + "@integration.test", "Transfer staff");
        UUID reviewerId = insertUser("transfer-reviewer-" + suffix + "@integration.test", "Transfer reviewer");
        assignRole(creatorId, "STAFF");
        assignRole(reviewerId, "SUPERVISOR");
        creator = authentication("transfer-staff-" + suffix + "@integration.test");
        reviewer = authentication("transfer-reviewer-" + suffix + "@integration.test");
        warehouseId = insertWarehouse("TRANSFER-WH-" + suffix);
        foreignWarehouseId = insertWarehouse("FOREIGN-WH-" + suffix);
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", creatorId, warehouseId);
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", reviewerId, warehouseId);
        sourceLocationId = insertLocation(warehouseId, "SOURCE-" + suffix);
        destinationLocationId = insertLocation(warehouseId, "DEST-" + suffix);
        foreignLocationId = insertLocation(foreignWarehouseId, "FOREIGN-" + suffix);
        UUID categoryId = jdbcTemplate.queryForObject("INSERT INTO product_categories(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "TRANSFER-CAT-" + suffix, "Transfer category");
        firstProductId = insertProduct(categoryId, "TRANSFER-SKU-A-" + suffix);
        secondProductId = insertProduct(categoryId, "TRANSFER-SKU-B-" + suffix);
        insertBalance(sourceLocationId, firstProductId, 20);
        insertBalance(sourceLocationId, secondProductId, 2);
    }

    @Test
    void approvalMovesStockAndWritesPairedMovements() {
        StockTransferResponse created = createTransfer(List.of(item(firstProductId, 5), item(secondProductId, 1)));
        transferService.submit(creator, created.id());

        BusinessException selfReview = assertThrows(BusinessException.class,
                () -> transferService.approve(creator, created.id()));
        assertEquals("TRANSFER_SELF_REVIEW_FORBIDDEN", selfReview.getCode());

        StockTransferResponse approved = transferService.approve(reviewer, created.id());
        assertEquals(StockTransferStatus.APPROVED, approved.status());
        assertEquals(15, stock(sourceLocationId, firstProductId));
        assertEquals(5, stock(destinationLocationId, firstProductId));
        assertEquals(20, stock(sourceLocationId, firstProductId) + stock(destinationLocationId, firstProductId));
        assertEquals(1, stock(sourceLocationId, secondProductId));
        assertEquals(1, stock(destinationLocationId, secondProductId));
        assertEquals(2, stock(sourceLocationId, secondProductId) + stock(destinationLocationId, secondProductId));
        assertEquals(4, count("SELECT count(*) FROM stock_movements WHERE source_entity_id = ?", created.id()));
        assertEquals(2, count("""
                SELECT count(*) FROM stock_movements WHERE source_entity_id = ? AND movement_type = 'STOCK_TRANSFER'
                    AND movement_direction = 'OUT' AND quantity IN (5, 1) AND stock_after IN (15, 1)
                """, created.id()));
        assertEquals(2, count("""
                SELECT count(*) FROM stock_movements WHERE source_entity_id = ? AND movement_type = 'STOCK_TRANSFER'
                    AND movement_direction = 'IN' AND quantity IN (5, 1) AND stock_after IN (5, 1)
                """, created.id()));
        assertEquals(1, count("SELECT count(*) FROM audit_trails WHERE entity_id = ? AND action = 'APPROVE'",
                created.id()));

        BusinessException secondApproval = assertThrows(BusinessException.class,
                () -> transferService.approve(reviewer, created.id()));
        assertEquals("TRANSFER_ALREADY_APPROVED", secondApproval.getCode());
        assertEquals(4, count("SELECT count(*) FROM stock_movements WHERE source_entity_id = ?", created.id()));
    }

    @Test
    void insufficientStockOnAnyLineRollsBackAllBalancesMovementsAndApproval() {
        StockTransferResponse created = createTransfer(List.of(item(firstProductId, 5), item(secondProductId, 9)));
        transferService.submit(creator, created.id());

        BusinessException insufficient = assertThrows(BusinessException.class,
                () -> transferService.approve(reviewer, created.id()));
        assertEquals("INSUFFICIENT_STOCK", insufficient.getCode());
        assertEquals(20, stock(sourceLocationId, firstProductId));
        assertEquals(2, stock(sourceLocationId, secondProductId));
        assertNull(optionalStock(destinationLocationId, firstProductId));
        assertNull(optionalStock(destinationLocationId, secondProductId));
        assertEquals(0, count("SELECT count(*) FROM stock_movements WHERE source_entity_id = ?", created.id()));
        assertEquals(0, count("SELECT count(*) FROM audit_trails WHERE entity_id = ? AND action = 'APPROVE'",
                created.id()));
        assertEquals(StockTransferStatus.PENDING, transferService.get(creator, created.id()).status());
    }

    @Test
    void rejectsDuplicateProductsEqualLocationsAndForeignLocations() {
        BusinessException sameLocation = assertThrows(BusinessException.class,
                () -> transferService.create(creator, new StockTransferCreateRequest(warehouseId, null,
                        List.of(new StockTransferItemRequest(firstProductId, sourceLocationId, sourceLocationId,
                                1, null)))));
        assertEquals("TRANSFER_LOCATIONS_MUST_DIFFER", sameLocation.getCode());

        BusinessException foreignLocation = assertThrows(BusinessException.class,
                () -> transferService.create(creator, new StockTransferCreateRequest(warehouseId, null,
                        List.of(new StockTransferItemRequest(firstProductId, sourceLocationId, foreignLocationId,
                                1, null)))));
        assertEquals("INVALID_TRANSFER_LOCATION", foreignLocation.getCode());

        BusinessException duplicateProduct = assertThrows(BusinessException.class,
                () -> createTransfer(List.of(item(firstProductId, 1), item(firstProductId, 2))));
        assertEquals("DUPLICATE_TRANSFER_PRODUCT", duplicateProduct.getCode());
    }

    @Test
    void rejectedTransferCanBeRevisedByItsCreator() {
        StockTransferResponse created = createTransfer(List.of(item(firstProductId, 4)));
        transferService.submit(creator, created.id());
        StockTransferResponse rejected = transferService.reject(reviewer, created.id(),
                new StockTransferRejectRequest("  Wrong quantity  "));
        assertEquals(StockTransferStatus.REJECTED, rejected.status());
        assertEquals("Wrong quantity", rejected.rejectionNote());
        StockTransferResponse revised = transferService.update(creator, created.id(),
                new StockTransferUpdateRequest("Corrected quantity", List.of(item(firstProductId, 6))));
        assertEquals(StockTransferStatus.PENDING, revised.status());
        assertNull(revised.rejectionNote());
        assertNull(revised.submittedAt());
        assertEquals(6, revised.items().get(0).quantity());
    }

    private StockTransferResponse createTransfer(List<StockTransferItemRequest> items) {
        return transferService.create(creator, new StockTransferCreateRequest(warehouseId, "transfer test", items));
    }

    private StockTransferItemRequest item(UUID productId, int quantity) {
        return new StockTransferItemRequest(productId, sourceLocationId, destinationLocationId, quantity, null);
    }

    private UUID insertUser(String email, String name) {
        return jdbcTemplate.queryForObject("INSERT INTO users(email, name, password) VALUES (?, ?, 'test') RETURNING id",
                UUID.class, email, name);
    }

    private void assignRole(UUID userId, String roleName) {
        UUID roleId = jdbcTemplate.queryForObject("INSERT INTO roles(name) VALUES (?) RETURNING id", UUID.class,
                roleName + "-" + UUID.randomUUID());
        jdbcTemplate.update("INSERT INTO user_roles(user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    private UUID insertWarehouse(String code) {
        return jdbcTemplate.queryForObject("INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id", UUID.class,
                code, code);
    }

    private UUID insertLocation(UUID warehouse, String code) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO warehouse_locations(warehouse_id, code, name) VALUES (?, ?, ?) RETURNING id
                """, UUID.class, warehouse, code, code);
    }

    private UUID insertProduct(UUID category, String sku) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO products(product_category_id, sku, name, unit) VALUES (?, ?, ?, 'pcs') RETURNING id
                """, UUID.class, category, sku, sku);
    }

    private void insertBalance(UUID location, UUID product, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO warehouse_location_items(warehouse_id, warehouse_location_id, product_id, quantity)
                VALUES (?, ?, ?, ?)
                """, warehouseId, location, product, quantity);
    }

    private int stock(UUID location, UUID product) {
        Integer stock = optionalStock(location, product);
        if (stock == null) throw new AssertionError("Expected a balance row.");
        return stock;
    }

    private Integer optionalStock(UUID location, UUID product) {
        List<Integer> stocks = jdbcTemplate.query("""
                SELECT quantity FROM warehouse_location_items
                WHERE warehouse_location_id = ? AND product_id = ? AND deleted_at IS NULL
                """, (row, index) -> row.getInt("quantity"), location, product);
        return stocks.isEmpty() ? null : stocks.get(0);
    }

    private int count(String sql, UUID transferId) {
        return jdbcTemplate.queryForObject(sql, Integer.class, transferId);
    }

    private static Authentication authentication(String email) {
        return new UsernamePasswordAuthenticationToken(email, "integration-test", List.of());
    }
}
