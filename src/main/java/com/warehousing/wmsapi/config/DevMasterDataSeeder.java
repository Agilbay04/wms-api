package com.warehousing.wmsapi.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "seeder.enable-db-setup", havingValue = "true")
public class DevMasterDataSeeder implements ApplicationRunner {
    private static final DateTimeFormatter RUN_TIMESTAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    private final JdbcTemplate jdbcTemplate;
    private final SeederProperties seederProperties;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public DevMasterDataSeeder(JdbcTemplate jdbcTemplate, SeederProperties seederProperties,
                               PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.seederProperties = seederProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        List<String[]> categories = readRows("categories.csv", 4);
        List<String[]> products = readRows("products.csv", 7);
        List<String[]> warehouses = readRows("warehouses.csv", 4);
        List<String[]> locations = readRows("locations.csv", 5);
        List<CreatedResource> created = transactionTemplate.execute(status ->
                seedRows(categories, products, warehouses, locations));
        writeResult(created);
    }

    private List<CreatedResource> seedRows(List<String[]> categories, List<String[]> products,
                                           List<String[]> warehouses, List<String[]> locations) {
        List<CreatedResource> created = new ArrayList<>();
        Map<String, UUID> categoryIds = new HashMap<>();
        for (String[] row : categories) {
            categoryIds.put(row[0], seedCategory(row, created));
        }
        Map<String, UUID> warehouseIds = new HashMap<>();
        for (String[] row : warehouses) {
            warehouseIds.put(row[0], seedWarehouse(row, created));
        }
        for (String[] row : products) {
            seedProduct(row, requireParent(categoryIds, row[1], "category"), created);
        }
        for (String[] row : locations) {
            seedLocation(row, requireParent(warehouseIds, row[0], "warehouse"), created);
        }
        return created;
    }

    private UUID seedCategory(String[] row, List<CreatedResource> created) {
        List<UUID> inserted = jdbcTemplate.query("""
                        INSERT INTO product_categories(code, name, description, is_active)
                        VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING RETURNING id
                        """, (rs, index) -> rs.getObject("id", UUID.class),
                row[0], row[1], row[2], Boolean.parseBoolean(row[3]));
        if (!inserted.isEmpty()) {
            created.add(resource(row[0], "product_categories", inserted.get(0)));
            return inserted.get(0);
        }
        return activeParentId("SELECT id FROM product_categories WHERE code = ? AND deleted_at IS NULL AND is_active",
                row[0], "category");
    }

    private UUID seedWarehouse(String[] row, List<CreatedResource> created) {
        List<UUID> inserted = jdbcTemplate.query("""
                        INSERT INTO warehouses(code, name, address, is_active)
                        VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING RETURNING id
                        """, (rs, index) -> rs.getObject("id", UUID.class),
                row[0], row[1], row[2], Boolean.parseBoolean(row[3]));
        if (!inserted.isEmpty()) {
            created.add(resource(row[0], "warehouses", inserted.get(0)));
            return inserted.get(0);
        }
        return activeParentId("SELECT id FROM warehouses WHERE code = ? AND deleted_at IS NULL AND is_active",
                row[0], "warehouse");
    }

    private void seedProduct(String[] row, UUID categoryId, List<CreatedResource> created) {
        List<UUID> inserted = jdbcTemplate.query("""
                        INSERT INTO products(product_category_id, sku, name, description, unit, minimum_stock, is_active)
                        VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (sku) DO NOTHING RETURNING id
                        """, (rs, index) -> rs.getObject("id", UUID.class),
                categoryId, row[0], row[2], row[3], row[4], Integer.parseInt(row[5]), Boolean.parseBoolean(row[6]));
        if (!inserted.isEmpty()) {
            created.add(resource(row[0], "products", inserted.get(0)));
        }
    }

    private void seedLocation(String[] row, UUID warehouseId, List<CreatedResource> created) {
        List<UUID> inserted = jdbcTemplate.query("""
                        INSERT INTO warehouse_locations(warehouse_id, code, name, description, is_active)
                        VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (warehouse_id, code) DO NOTHING RETURNING id
                        """, (rs, index) -> rs.getObject("id", UUID.class),
                warehouseId, row[1], row[2], row[3], Boolean.parseBoolean(row[4]));
        if (!inserted.isEmpty()) {
            created.add(resource(row[0] + "/" + row[1], "warehouse_locations", inserted.get(0)));
        }
    }

    private UUID activeParentId(String query, String code, String kind) {
        try {
            return jdbcTemplate.queryForObject(query, UUID.class, code);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalStateException("Seed " + kind + " " + code + " already exists but is inactive or deleted.",
                    exception);
        }
    }

    private static UUID requireParent(Map<String, UUID> ids, String code, String kind) {
        UUID id = ids.get(code);
        if (id == null) {
            throw new IllegalArgumentException("Unknown seed " + kind + " code: " + code);
        }
        return id;
    }

    static List<String[]> readRows(String filename, int columns) throws IOException {
        ClassPathResource resource = new ClassPathResource("seed/" + filename);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8))) {
            if (reader.readLine() == null) {
                throw new IllegalArgumentException("Seed CSV is empty: " + filename);
            }
            List<String[]> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] values = line.split(";", -1);
                if (values.length != columns) {
                    throw new IllegalArgumentException("Invalid seed CSV row in " + filename + ": " + line);
                }
                rows.add(values);
            }
            return rows;
        }
    }

    private static CreatedResource resource(String key, String table, UUID id) {
        return new CreatedResource(key, table, "created-db", id,
                new Cleanup("db", table, "id"));
    }

    private void writeResult(List<CreatedResource> created) throws IOException {
        Instant generatedAt = Instant.now();
        Path directory = Path.of(seederProperties.resultDirectory(), "seed-runs",
                RUN_TIMESTAMP.format(generatedAt) + "-" + UUID.randomUUID());
        Files.createDirectories(directory);
        Path result = directory.resolve("seed-result.json");
        Files.writeString(result, jsonMapper.writeValueAsString(
                new SeedResult("master-data", generatedAt.toString(), created)),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    private record SeedResult(String module, String generatedAt, List<CreatedResource> resources) {
    }

    private record CreatedResource(String key, String target, String action, UUID id, Cleanup cleanup) {
    }

    private record Cleanup(String type, String table, String primaryKey) {
    }
}
