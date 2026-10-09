package com.warehousing.wmsapi.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevMasterDataSeederTest {

    @TempDir
    Path exportDirectory;

    @Test
    void onlyRunsWithDevProfileAndExplicitDbSetupFlag() {
        Profile profile = DevMasterDataSeeder.class.getAnnotation(Profile.class);
        ConditionalOnProperty condition = DevMasterDataSeeder.class.getAnnotation(ConditionalOnProperty.class);

        assertEquals("dev", profile.value()[0]);
        assertEquals("seeder.enable-db-setup", condition.name()[0]);
        assertEquals("true", condition.havingValue());
    }

    @Test
    void csvFixturesContainEnoughProductsForSecondPage() throws IOException {
        assertEquals(3, DevMasterDataSeeder.readRows("categories.csv", 4).size());
        assertEquals(12, DevMasterDataSeeder.readRows("products.csv", 7).size());
        assertEquals(2, DevMasterDataSeeder.readRows("warehouses.csv", 4).size());
        assertEquals(4, DevMasterDataSeeder.readRows("locations.csv", 5).size());
    }

    @Test
    void seedsInDependencyOrderAndDoesNotDuplicateExistingRows() throws Exception {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        PlatformTransactionManager transactionManager = transactionManager();
        DevMasterDataSeeder seeder = new DevMasterDataSeeder(new DevMasterDataRepository(jdbcTemplate),
                new SeederProperties(true, exportDirectory.toString()), transactionManager);

        seeder.run(mock(ApplicationArguments.class));
        seeder.run(mock(ApplicationArguments.class));

        assertEquals(42, jdbcTemplate.insertTables.size());
        assertEquals(List.of("product_categories", "product_categories", "product_categories",
                "warehouses", "warehouses"), jdbcTemplate.insertTables.subList(0, 5));
        assertEquals("products", jdbcTemplate.insertTables.get(5));
        assertEquals("warehouse_locations", jdbcTemplate.insertTables.get(17));
        assertEquals(21, jdbcTemplate.rows.size());
        verify(transactionManager, times(2)).commit(any());

        List<String> results;
        try (var files = Files.walk(exportDirectory)) {
            results = files.filter(path -> path.getFileName().toString().equals("seed-result.json"))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();
        }
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(result -> result.split("created-db", -1).length - 1 == 21));
        assertTrue(results.stream().anyMatch(result -> result.contains("\"resources\":[]")));
        assertTrue(results.stream().noneMatch(result -> result.contains("password")));
    }

    @Test
    void doesNotWriteResultWhenDatabaseCommitFails() throws Exception {
        PlatformTransactionManager transactionManager = transactionManager();
        doThrow(new IllegalStateException("commit failed")).when(transactionManager).commit(any());
        DevMasterDataSeeder seeder = new DevMasterDataSeeder(new DevMasterDataRepository(new RecordingJdbcTemplate()),
                new SeederProperties(true, exportDirectory.toString()), transactionManager);

        assertThrows(IllegalStateException.class, () -> seeder.run(mock(ApplicationArguments.class)));
        try (var files = Files.walk(exportDirectory)) {
            assertEquals(0, files.filter(path -> path.getFileName().toString().equals("seed-result.json")).count());
        }
    }

    private static PlatformTransactionManager transactionManager() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return transactionManager;
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private final Map<String, UUID> rows = new HashMap<>();
        private final List<String> insertTables = new ArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            String table = tableFromInsert(sql);
            insertTables.add(table);
            String key = keyFor(table, args);
            if (rows.containsKey(key)) {
                return List.of();
            }
            UUID id = UUID.randomUUID();
            rows.put(key, id);
            return (List<T>) List.of(id);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            String table = sql.contains("product_categories") ? "product_categories" : "warehouses";
            return (T) rows.get(table + ":" + args[0]);
        }

        private static String tableFromInsert(String sql) {
            for (String table : List.of("product_categories", "products", "warehouses", "warehouse_locations")) {
                if (sql.contains("INSERT INTO " + table + "(")) {
                    return table;
                }
            }
            throw new IllegalArgumentException("Unknown seed insert");
        }

        private static String keyFor(String table, Object[] args) {
            return switch (table) {
                case "product_categories", "warehouses" -> table + ":" + args[0];
                case "products" -> table + ":" + args[1];
                case "warehouse_locations" -> table + ":" + args[0] + ":" + args[1];
                default -> throw new IllegalArgumentException("Unknown seed table");
            };
        }
    }
}
