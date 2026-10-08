package com.warehousing.wmsapi.inbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.inbound.dto.InboundCreateRequest;
import com.warehousing.wmsapi.inbound.dto.InboundItemRequest;
import com.warehousing.wmsapi.inbound.dto.InboundPutawayItemRequest;
import com.warehousing.wmsapi.inbound.dto.InboundPutawayRequest;
import com.warehousing.wmsapi.inbound.dto.InboundRejectRequest;
import com.warehousing.wmsapi.inbound.dto.InboundResponse;
import com.warehousing.wmsapi.inbound.dto.InboundUpdateRequest;
import com.warehousing.wmsapi.inbound.enums.InboundStatus;
import com.warehousing.wmsapi.inbound.service.InboundService;
import com.warehousing.wmsapi.common.security.PermissionService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EnabledIf("isTestDatabaseConfigured")
class InboundIntegrationTest {
    private static final String EXPECTED_DATABASE = "wms-integration-test";
    private static final DatabaseSettings DATABASE = DatabaseSettings.fromEnvironment();
    private static final String SCHEMA = "wms_inbound_it_" + UUID.randomUUID().toString().replace("-", "");

    @Autowired
    private InboundService inboundService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    private UUID warehouseId;
    private UUID otherWarehouseId;
    private UUID locationId;
    private UUID otherLocationId;
    private UUID firstProductId;
    private UUID secondProductId;
    private Authentication creator;
    private Authentication reviewer;

    public static boolean isTestDatabaseConfigured() {
        return DATABASE != null;
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        if (DATABASE == null) {
            return;
        }
        if (!EXPECTED_DATABASE.equals(DATABASE.database())) {
            throw new IllegalStateException("WMS_TEST_DB_NAME must be wms-integration-test for inbound tests.");
        }
        createSchema();
        String jdbcUrl = "jdbc:postgresql://" + DATABASE.host() + ":" + DATABASE.port()
                + "/" + DATABASE.database() + "?currentSchema=" + SCHEMA;
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", DATABASE::username);
        registry.add("spring.datasource.password", DATABASE::password);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.flyway.create-schemas", () -> "false");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        registry.add("spring.cache.type", () -> "simple");
    }

    @AfterAll
    static void dropSchema() throws Exception {
        if (DATABASE == null) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(DATABASE.jdbcUrl(), DATABASE.username(),
                DATABASE.password()); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID staffId = insertUser("staff-" + suffix + "@integration.test", "Staff");
        UUID supervisorId = insertUser("supervisor-" + suffix + "@integration.test", "Supervisor");
        UUID staffRoleId = roleId("STAFF");
        UUID supervisorRoleId = roleId("SUPERVISOR");
        jdbcTemplate.update("INSERT INTO user_roles(user_id, role_id) VALUES (?, ?)", staffId, staffRoleId);
        jdbcTemplate.update("INSERT INTO user_roles(user_id, role_id) VALUES (?, ?)", supervisorId, supervisorRoleId);
        creator = authentication("staff-" + suffix + "@integration.test");
        reviewer = authentication("supervisor-" + suffix + "@integration.test");

        warehouseId = insertWarehouse("WH-" + suffix);
        otherWarehouseId = insertWarehouse("OTHER-" + suffix);
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", staffId, warehouseId);
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", supervisorId,
                warehouseId);
        locationId = insertLocation(warehouseId, "LOC-" + suffix);
        otherLocationId = insertLocation(otherWarehouseId, "OTHER-LOC-" + suffix);
        UUID categoryId = jdbcTemplate.queryForObject("""
                INSERT INTO product_categories(code, name) VALUES (?, ?) RETURNING id
                """, UUID.class, "CAT-" + suffix, "Integration category");
        firstProductId = insertProduct(categoryId, "SKU-A-" + suffix);
        secondProductId = insertProduct(categoryId, "SKU-B-" + suffix);
        seedInboundPermissions();
    }

    @Test
    void approvalDoesNotChangeStockAndPutawayPostsStockExactlyOnce() {
        InboundResponse created = createInbound();
        InboundResponse submitted = inboundService.submit(creator, created.id());
        assertEquals(InboundStatus.PENDING, submitted.status());

        BusinessException selfReview = assertThrows(BusinessException.class,
                () -> inboundService.approve(creator, created.id()));
        assertEquals("INBOUND_SELF_REVIEW_FORBIDDEN", selfReview.getCode());

        InboundResponse approved = inboundService.approve(reviewer, created.id());
        assertEquals(InboundStatus.APPROVED, approved.status());
        assertEquals(0, balanceCount(created.id()));
        assertEquals(0, movementCount(created.id()));

        InboundResponse putaway = inboundService.putaway(creator, created.id(), putawayRequest(created, locationId));
        assertEquals(InboundStatus.APPROVED, putaway.status());
        assertEquals(5, stock(firstProductId, locationId));
        assertEquals(7, stock(secondProductId, locationId));
        assertEquals(2, movementCount(created.id()));
        assertEquals(2, count("""
                SELECT count(*) FROM stock_movements
                WHERE source_entity_id = ? AND movement_type = 'INBOUND' AND movement_direction = 'IN'
                  AND quantity IN (5, 7) AND stock_after IN (5, 7)
                """, created.id()));
        assertEquals(1, count("SELECT count(*) FROM audit_trails WHERE entity_id = ? AND action = 'APPROVE'",
                created.id()));

        BusinessException duplicatePutaway = assertThrows(BusinessException.class,
                () -> inboundService.putaway(creator, created.id(), putawayRequest(created, locationId)));
        assertEquals("INBOUND_ALREADY_PUT_AWAY", duplicatePutaway.getCode());
        assertEquals(5, stock(firstProductId, locationId));
        assertEquals(7, stock(secondProductId, locationId));
        assertEquals(2, movementCount(created.id()));
    }

    @Test
    void rejectedInboundCanOnlyBeRevisedByItsCreatorAndThenResubmitted() {
        InboundResponse created = createInbound();
        inboundService.submit(creator, created.id());
        InboundResponse rejected = inboundService.reject(reviewer, created.id(),
                new InboundRejectRequest("  Quantity needs confirmation  "));
        assertEquals(InboundStatus.REJECTED, rejected.status());
        assertEquals("Quantity needs confirmation", rejected.rejectionNote());
        assertEquals(0, movementCount(created.id()));

        UUID otherStaffId = insertUser("other-staff-" + UUID.randomUUID() + "@integration.test", "Other staff");
        jdbcTemplate.update("INSERT INTO user_roles(user_id, role_id) VALUES (?, ?)", otherStaffId, roleId("STAFF"));
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", otherStaffId, warehouseId);
        Authentication otherStaff = authentication(jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?", String.class, otherStaffId));
        BusinessException notOwner = assertThrows(BusinessException.class, () -> inboundService.update(
                otherStaff, created.id(), new InboundUpdateRequest("PO-OTHER", null,
                        List.of(new InboundItemRequest(firstProductId, 8, null)))));
        assertEquals("INBOUND_OWNER_REQUIRED", notOwner.getCode());

        InboundResponse revised = inboundService.update(creator, created.id(), new InboundUpdateRequest(
                "PO-REVISED", "updated after rejection", List.of(new InboundItemRequest(firstProductId, 8, null))));
        assertEquals(InboundStatus.PENDING, revised.status());
        assertNull(revised.rejectionNote());
        assertNull(revised.submittedAt());
        assertNull(revised.reviewedAt());
        assertEquals("PO-REVISED", revised.purchaseOrderNumber());
        assertEquals(1, revised.items().size());
        assertEquals(8, revised.items().get(0).quantity());
        assertEquals(InboundStatus.PENDING, inboundService.submit(creator, created.id()).status());
    }

    @Test
    void inboundRolePermissionsSeparateStaffActionsFromSupervisorReview() {
        assertTrue(permissionService.hasPermission(creator, "INBOUNDS", "CREATE"));
        assertTrue(permissionService.hasPermission(creator, "INBOUNDS", "UPDATE"));
        assertFalse(permissionService.hasPermission(creator, "INBOUNDS", "APPROVE"));
        assertFalse(permissionService.hasPermission(creator, "INBOUNDS", "REJECT"));
        assertTrue(permissionService.hasPermission(reviewer, "INBOUNDS", "READ"));
        assertTrue(permissionService.hasPermission(reviewer, "INBOUNDS", "APPROVE"));
        assertTrue(permissionService.hasPermission(reviewer, "INBOUNDS", "REJECT"));
        assertFalse(permissionService.hasPermission(reviewer, "INBOUNDS", "CREATE"));
    }

    @Test
    void putawayRequiresApprovalAndActiveProducts() {
        InboundResponse created = inboundService.create(creator, new InboundCreateRequest(warehouseId,
                "PO-INACTIVE", "inactive product case", List.of(new InboundItemRequest(firstProductId, 3, null))));
        BusinessException notApproved = assertThrows(BusinessException.class,
                () -> inboundService.putaway(creator, created.id(), new InboundPutawayRequest(List.of(
                        new InboundPutawayItemRequest(created.items().get(0).id(), locationId)))));
        assertEquals("INBOUND_NOT_APPROVED", notApproved.getCode());

        inboundService.submit(creator, created.id());
        inboundService.approve(reviewer, created.id());
        jdbcTemplate.update("UPDATE products SET is_active = false WHERE id = ?", firstProductId);
        BusinessException inactiveProduct = assertThrows(BusinessException.class,
                () -> inboundService.putaway(creator, created.id(), new InboundPutawayRequest(List.of(
                        new InboundPutawayItemRequest(created.items().get(0).id(), locationId)))));
        assertEquals("INACTIVE_PRODUCT", inactiveProduct.getCode());
        assertEquals(0, balanceCount(created.id()));
        assertEquals(0, movementCount(created.id()));
    }

    @Test
    void invalidDestinationRollsBackEarlierItemStockAndMovements() {
        InboundResponse created = inboundService.create(creator, new InboundCreateRequest(warehouseId,
                "PO-ROLLBACK", "rollback case", List.of(
                new InboundItemRequest(firstProductId, 5, null),
                new InboundItemRequest(secondProductId, 7, null))));
        inboundService.submit(creator, created.id());
        inboundService.approve(reviewer, created.id());
        InboundPutawayRequest request = putawayRequest(created, locationId, otherLocationId);

        BusinessException invalidLocation = assertThrows(BusinessException.class,
                () -> inboundService.putaway(creator, created.id(), request));
        assertEquals("INVALID_PUTAWAY_LOCATION", invalidLocation.getCode());
        assertEquals(0, balanceCount(created.id()));
        assertEquals(0, movementCount(created.id()));
        assertEquals(0, count("SELECT count(*) FROM audit_trails WHERE entity_id = ? AND description = 'Inbound putaway completed.'",
                created.id()));
        assertEquals(InboundStatus.APPROVED, inboundService.get(creator, created.id()).status());
    }

    private InboundResponse createInbound() {
        return inboundService.create(creator, new InboundCreateRequest(warehouseId, "PO-SUCCESS", "integration case",
                List.of(new InboundItemRequest(firstProductId, 5, null),
                        new InboundItemRequest(secondProductId, 7, null))));
    }

    private InboundPutawayRequest putawayRequest(InboundResponse inbound, UUID firstLocation) {
        return putawayRequest(inbound, firstLocation, firstLocation);
    }

    private InboundPutawayRequest putawayRequest(InboundResponse inbound, UUID firstLocation, UUID secondLocation) {
        assertEquals(2, inbound.items().size());
        return new InboundPutawayRequest(List.of(
                new InboundPutawayItemRequest(inbound.items().get(0).id(), firstLocation),
                new InboundPutawayItemRequest(inbound.items().get(1).id(), secondLocation)));
    }

    private UUID insertUser(String email, String name) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO users(email, name, password) VALUES (?, ?, 'test') RETURNING id
                """, UUID.class, email, name);
    }

    private UUID roleId(String name) {
        List<UUID> roleIds = jdbcTemplate.query("SELECT id FROM roles WHERE name = ?", (row, index) ->
                row.getObject("id", UUID.class), name);
        if (!roleIds.isEmpty()) {
            return roleIds.get(0);
        }
        return jdbcTemplate.queryForObject("INSERT INTO roles(name) VALUES (?) RETURNING id", UUID.class, name);
    }

    private UUID insertWarehouse(String code) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id
                """, UUID.class, code, code);
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

    private void seedInboundPermissions() {
        jdbcTemplate.update("INSERT INTO resources(name) VALUES ('INBOUNDS') ON CONFLICT (name) DO NOTHING");
        jdbcTemplate.update("INSERT INTO operations(name) VALUES ('CREATE'), ('READ'), ('UPDATE'), ('APPROVE'), ('REJECT') "
                + "ON CONFLICT (name) DO NOTHING");
        jdbcTemplate.update("""
                INSERT INTO permissions(operation_id, resource_id)
                SELECT o.id, r.id FROM operations o CROSS JOIN resources r
                WHERE r.name = 'INBOUNDS' AND o.name IN ('CREATE', 'READ', 'UPDATE', 'APPROVE', 'REJECT')
                ON CONFLICT (operation_id, resource_id) DO NOTHING
                """);
        grant("STAFF", "CREATE", "READ", "UPDATE");
        grant("SUPERVISOR", "READ", "APPROVE", "REJECT");
    }

    private void grant(String role, String... operations) {
        for (String operation : operations) {
            jdbcTemplate.update("""
                    INSERT INTO role_permissions(role_id, permission_id)
                    SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
                    JOIN resources resource ON resource.id = p.resource_id
                    JOIN operations op ON op.id = p.operation_id
                    WHERE r.name = ? AND resource.name = 'INBOUNDS' AND op.name = ?
                    ON CONFLICT (role_id, permission_id) DO NOTHING
                    """, role, operation);
        }
    }

    private int balanceCount(UUID inboundId) {
        return count("""
                SELECT count(*) FROM warehouse_location_items wli
                WHERE wli.product_id IN (SELECT product_id FROM inbound_items WHERE inbound_id = ?)
                """, inboundId);
    }

    private int movementCount(UUID inboundId) {
        return count("SELECT count(*) FROM stock_movements WHERE source_entity_id = ? AND deleted_at IS NULL",
                inboundId);
    }

    private int stock(UUID productId, UUID destinationId) {
        Integer quantity = jdbcTemplate.queryForObject("""
                SELECT quantity FROM warehouse_location_items
                WHERE product_id = ? AND warehouse_location_id = ? AND deleted_at IS NULL
                """, Integer.class, productId, destinationId);
        assertNotNull(quantity);
        return quantity;
    }

    private int count(String sql, UUID id) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        assertNotNull(count);
        return count;
    }

    private static Authentication authentication(String email) {
        return new UsernamePasswordAuthenticationToken(email, "integration-test", List.of());
    }

    private static void createSchema() {
        if (!EXPECTED_DATABASE.equals(DATABASE.database())) {
            throw new IllegalStateException("WMS_TEST_DB_NAME must be wms-integration-test for inbound tests.");
        }
        try (Connection connection = DriverManager.getConnection(DATABASE.jdbcUrl(), DATABASE.username(),
                DATABASE.password()); Statement statement = connection.createStatement()) {
            if (!EXPECTED_DATABASE.equals(connection.getCatalog())) {
                throw new IllegalStateException("Connected database must match WMS_TEST_DB_NAME.");
            }
            statement.execute("CREATE SCHEMA " + SCHEMA);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create isolated inbound integration schema.", exception);
        }
    }

    private record DatabaseSettings(String host, int port, String database, String username, String password) {
        private static DatabaseSettings fromEnvironment() {
            String host = System.getenv("WMS_TEST_DB_HOST");
            String database = System.getenv("WMS_TEST_DB_NAME");
            String username = System.getenv("WMS_TEST_DB_USERNAME");
            if (isBlank(host) || isBlank(database) || isBlank(username)) {
                return null;
            }
            int port = Integer.parseInt(System.getenv().getOrDefault("WMS_TEST_DB_PORT", "5432"));
            return new DatabaseSettings(host, port, database, username,
                    System.getenv().getOrDefault("WMS_TEST_DB_PASSWORD", ""));
        }

        private String jdbcUrl() {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database;
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}
