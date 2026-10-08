package com.warehousing.wmsapi.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

class FlywayMigrationIntegrationTest {
    private static final String TEST_DATABASE_NAME = "wms-integration-test";
    private static final List<String> REQUIRED_TABLES = List.of(
            "users",
            "warehouse_location_items",
            "stock_movements",
            "audit_trails",
            "export_jobs"
    );

    @Test
    void migratesAllVersionsInAnIsolatedSchema() throws Exception {
        DatabaseSettings settings = DatabaseSettings.fromEnvironment();
        if (settings == null) {
            assumeTrue(false,
                    "Set WMS_TEST_DB_HOST, WMS_TEST_DB_NAME, and WMS_TEST_DB_USERNAME to opt in.");
            return;
        }
        if (!TEST_DATABASE_NAME.equals(settings.database())) {
            throw new IllegalStateException(
                    "WMS_TEST_DB_NAME must be wms-integration-test for migration tests.");
        }

        String jdbcUrl = "jdbc:postgresql://" + settings.host() + ":" + settings.port()
                + "/" + settings.database();
        String schema = "wms_it_" + UUID.randomUUID().toString().replace("-", "");
        boolean schemaCreated = false;
        try {
            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl, settings.username(), settings.password());
                 Statement statement = connection.createStatement()) {
                assertEquals(settings.database(), connection.getCatalog(),
                        "Connected database must match WMS_TEST_DB_NAME.");
                statement.execute("CREATE SCHEMA " + schema);
                schemaCreated = true;
                assertEquals(0, schemaTableCount(connection, schema),
                        "The isolated migration schema must be empty before Flyway runs.");
            }

            Flyway flyway = Flyway.configure()
                    .dataSource(jdbcUrl, settings.username(), settings.password())
                    .schemas(schema)
                    .defaultSchema(schema)
                    .createSchemas(false)
                    .locations("classpath:db/migration")
                    .load();
            MigrateResult result = flyway.migrate();

            assertEquals(9, result.migrationsExecuted, "Expected all nine checked-in migrations to run.");
            flyway.validate();
            assertEquals("9", flyway.info().current().getVersion().getVersion());

            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl, settings.username(), settings.password())) {
                for (String table : REQUIRED_TABLES) {
                    assertTrue(tableExists(connection, schema, table), "Expected migrated table: " + table);
                }
            }
        } finally {
            if (schemaCreated) {
                try (Connection connection = DriverManager.getConnection(
                        jdbcUrl, settings.username(), settings.password());
                     Statement statement = connection.createStatement()) {
                    statement.execute("DROP SCHEMA " + schema + " CASCADE");
                }
            }
        }
    }

    private static int schemaTableCount(Connection connection, String schema) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT count(*)
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_type = 'BASE TABLE'
                """)) {
            statement.setString(1, schema);
            try (ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
            }
        }
    }

    private static boolean tableExists(Connection connection, String schema, String table) throws Exception {
        try (ResultSet result = connection.getMetaData().getTables(
                connection.getCatalog(), schema, table, new String[] {"TABLE"})) {
            return result.next();
        }
    }

    private record DatabaseSettings(String host, int port, String database, String username, String password) {
        private static DatabaseSettings fromEnvironment() {
            String database = System.getenv("WMS_TEST_DB_NAME");
            String host = System.getenv("WMS_TEST_DB_HOST");
            String username = System.getenv("WMS_TEST_DB_USERNAME");
            if (isBlank(host) || isBlank(database) || isBlank(username)) {
                return null;
            }
            String portValue = System.getenv().getOrDefault("WMS_TEST_DB_PORT", "5432");
            String password = System.getenv().getOrDefault("WMS_TEST_DB_PASSWORD", "");
            return new DatabaseSettings(host, Integer.parseInt(portValue), database, username, password);
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}
