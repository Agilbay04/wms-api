package com.warehousing.wmsapi.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.warehousing.wmsapi.audit.dto.AuditTrailRequest;
import com.warehousing.wmsapi.persistence.IntegrationTestDatabase;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
class AuditTrailIntegrationTest {
    private static final String SCHEMA = IntegrationTestDatabase.newSchema("wms_audit_it_");

    @Autowired private AuditTrailService auditTrailService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID warehouseId;
    private UUID outboundId;
    private Authentication authentication;

    public static boolean isTestDatabaseConfigured() { return IntegrationTestDatabase.isConfigured(); }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        IntegrationTestDatabase.configure(registry, SCHEMA);
    }

    @AfterAll
    static void cleanSchema() throws Exception { IntegrationTestDatabase.dropSchema(SCHEMA); }

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "audit-user-" + suffix + "@integration.test";
        userId = jdbcTemplate.queryForObject("INSERT INTO users(email, name, password) VALUES (?, ?, 'test') RETURNING id",
                UUID.class, email, "Audit user");
        authentication = new UsernamePasswordAuthenticationToken(email, "integration-test", List.of());
        warehouseId = insertWarehouse("AUDIT-WH-" + suffix);
        UUID foreignWarehouseId = insertWarehouse("AUDIT-FOREIGN-" + suffix);
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", userId, warehouseId);
        outboundId = insertOutbound(warehouseId, userId, "AUDIT-OUT-" + suffix);
        UUID foreignOutboundId = insertOutbound(foreignWarehouseId, userId, "AUDIT-FOREIGN-OUT-" + suffix);
        insertAudit(userId, outboundId, "CREATE", "Accessible event");
        insertAudit(userId, outboundId, "APPROVE", "Accessible approval");
        insertAudit(userId, foreignOutboundId, "APPROVE", "Must remain hidden");
    }

    @Test
    void auditFiltersApplyWithBoundedPaginationAndOnlyVisibleWarehouseEvents() {
        AuditTrailRequest request = new AuditTrailRequest();
        request.setEntityType("outbound");
        request.setUserId(userId);
        request.setFromDate(LocalDate.now(ZoneOffset.UTC));
        request.setToDate(LocalDate.now(ZoneOffset.UTC));
        request.setSize(1);

        var firstPage = auditTrailService.list(authentication, request);
        assertEquals(2, firstPage.totalElements());
        assertEquals(1, firstPage.content().size());
        assertEquals("OUTBOUND", firstPage.content().get(0).entityType());
        assertEquals(userId, firstPage.content().get(0).userId());

        request.setPage(2);
        var secondPage = auditTrailService.list(authentication, request);
        assertEquals(2, secondPage.totalElements());
        assertEquals(1, secondPage.content().size());

        request.setAction("APPROVE");
        request.setPage(1);
        var filtered = auditTrailService.list(authentication, request);
        assertEquals(1, filtered.totalElements());
        assertEquals("APPROVE", filtered.content().get(0).action());
        assertEquals(outboundId, filtered.content().get(0).entityId());
    }

    private UUID insertWarehouse(String code) {
        return jdbcTemplate.queryForObject("INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, code, code);
    }

    private UUID insertOutbound(UUID warehouse, UUID actor, String reference) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO outbounds(warehouse_id, reference_number, status, created_by_user_id)
                VALUES (?, ?, 'PENDING', ?) RETURNING id
                """, UUID.class, warehouse, reference, actor);
    }

    private void insertAudit(UUID actor, UUID entityId, String action, String description) {
        jdbcTemplate.update("""
                INSERT INTO audit_trails(user_id, action, entity_type, entity_id, description)
                VALUES (?, ?, 'OUTBOUND', ?, ?)
                """, actor, action, entityId, description);
    }
}
