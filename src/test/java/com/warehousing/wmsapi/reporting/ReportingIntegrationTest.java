package com.warehousing.wmsapi.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.warehousing.wmsapi.outbound.dto.OutboundCreateRequest;
import com.warehousing.wmsapi.outbound.dto.OutboundItemRequest;
import com.warehousing.wmsapi.outbound.service.OutboundService;
import com.warehousing.wmsapi.persistence.IntegrationTestDatabase;
import com.warehousing.wmsapi.reporting.dto.StockReportRequest;
import com.warehousing.wmsapi.reporting.dto.StockMovementReportRequest;
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
class ReportingIntegrationTest {
    private static final String SCHEMA = IntegrationTestDatabase.newSchema("wms_reporting_it_");

    @Autowired private DashboardService dashboardService;
    @Autowired private StockReportService stockReportService;
    @Autowired private OutboundService outboundService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID warehouseId;
    private UUID locationId;
    private UUID productId;
    private Authentication creator;
    private Authentication reviewer;

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
        UUID creatorId = insertUser("report-staff-" + suffix + "@integration.test");
        UUID reviewerId = insertUser("report-reviewer-" + suffix + "@integration.test");
        creator = authentication("report-staff-" + suffix + "@integration.test");
        reviewer = authentication("report-reviewer-" + suffix + "@integration.test");
        warehouseId = jdbcTemplate.queryForObject("INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "REPORT-WH-" + suffix, "Report warehouse");
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", creatorId, warehouseId);
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", reviewerId, warehouseId);
        locationId = jdbcTemplate.queryForObject("INSERT INTO warehouse_locations(warehouse_id, code, name) VALUES (?, ?, ?) RETURNING id",
                UUID.class, warehouseId, "REPORT-LOC-" + suffix, "Report location");
        UUID categoryId = jdbcTemplate.queryForObject("INSERT INTO product_categories(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "REPORT-CAT-" + suffix, "Report category");
        productId = jdbcTemplate.queryForObject("INSERT INTO products(product_category_id, sku, name, unit, minimum_stock) VALUES (?, ?, ?, 'pcs', 3) RETURNING id",
                UUID.class, categoryId, "REPORT-SKU-" + suffix, "Reported product");
        jdbcTemplate.update("INSERT INTO warehouse_location_items(warehouse_id, warehouse_location_id, product_id, quantity) VALUES (?, ?, ?, 5)",
                warehouseId, locationId, productId);
    }

    @Test
    void dashboardAndSummaryRefreshAfterApprovedOutboundAndReportsHonorWarehouseAndDateFilters() {
        assertEquals(5, dashboardService.get(creator, warehouseId).totalStock());
        assertEquals(5, stockReportService.summary(creator, warehouseId).totalStock());

        var outbound = outboundService.create(creator, new OutboundCreateRequest(warehouseId, "report test",
                List.of(new OutboundItemRequest(productId, locationId, 2, null))));
        outboundService.submit(creator, outbound.id());
        assertEquals(1, dashboardService.get(creator, warehouseId).pendingTransactions().get("outbounds"));
        outboundService.approve(reviewer, outbound.id());

        assertEquals(3, dashboardService.get(creator, warehouseId).totalStock());
        assertEquals(0, dashboardService.get(creator, warehouseId).pendingTransactions().get("outbounds"));
        assertEquals(3, stockReportService.summary(creator, warehouseId).totalStock());

        StockReportRequest allStocks = new StockReportRequest();
        allStocks.setWarehouseId(warehouseId);
        assertEquals(1, stockReportService.stocks(creator, allStocks).totalElements());

        StockMovementReportRequest movements = new StockMovementReportRequest();
        movements.setWarehouseId(warehouseId);
        movements.setFromDate(java.time.LocalDate.now(java.time.ZoneOffset.UTC));
        movements.setToDate(java.time.LocalDate.now(java.time.ZoneOffset.UTC));
        assertEquals(1, stockReportService.movements(creator, movements).totalElements());

        StockMovementReportRequest invertedDates = new StockMovementReportRequest();
        invertedDates.setFromDate(java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1));
        invertedDates.setToDate(java.time.LocalDate.now(java.time.ZoneOffset.UTC));
        org.junit.jupiter.api.Assertions.assertEquals("INVALID_DATE_RANGE",
                org.junit.jupiter.api.Assertions.assertThrows(com.warehousing.wmsapi.common.error.BusinessException.class,
                        () -> stockReportService.movements(creator, invertedDates)).getCode());
    }

    @Test
    void dashboardAndReportsDenyWarehouseOutsideUserAssignments() {
        UUID foreignWarehouse = jdbcTemplate.queryForObject("INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "REPORT-FOREIGN-" + UUID.randomUUID(), "Foreign warehouse");
        org.junit.jupiter.api.Assertions.assertEquals("WAREHOUSE_FORBIDDEN",
                org.junit.jupiter.api.Assertions.assertThrows(com.warehousing.wmsapi.common.error.BusinessException.class,
                        () -> dashboardService.get(creator, foreignWarehouse)).getCode());
        StockReportRequest request = new StockReportRequest();
        request.setWarehouseId(foreignWarehouse);
        org.junit.jupiter.api.Assertions.assertEquals("WAREHOUSE_FORBIDDEN",
                org.junit.jupiter.api.Assertions.assertThrows(com.warehousing.wmsapi.common.error.BusinessException.class,
                        () -> stockReportService.stocks(creator, request)).getCode());
    }

    private UUID insertUser(String email) {
        return jdbcTemplate.queryForObject("INSERT INTO users(email, name, password) VALUES (?, ?, 'test') RETURNING id",
                UUID.class, email, email);
    }

    private static Authentication authentication(String email) {
        return new UsernamePasswordAuthenticationToken(email, "integration-test", List.of());
    }
}
