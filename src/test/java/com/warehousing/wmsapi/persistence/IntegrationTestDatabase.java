package com.warehousing.wmsapi.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class IntegrationTestDatabase {
    private static final String EXPECTED_DATABASE = "wms-integration-test";
    private static final Settings SETTINGS = Settings.fromEnvironment();

    private IntegrationTestDatabase() {
    }

    public static boolean isConfigured() {
        return SETTINGS != null;
    }

    public static String newSchema(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    public static void configure(DynamicPropertyRegistry registry, String schema) {
        if (SETTINGS == null) {
            return;
        }
        if (!EXPECTED_DATABASE.equals(SETTINGS.database())) {
            throw new IllegalStateException("WMS_TEST_DB_NAME must be wms-integration-test for integration tests.");
        }
        try (Connection connection = DriverManager.getConnection(SETTINGS.jdbcUrl(), SETTINGS.username(),
                SETTINGS.password()); Statement statement = connection.createStatement()) {
            if (!EXPECTED_DATABASE.equals(connection.getCatalog())) {
                throw new IllegalStateException("Connected database must match WMS_TEST_DB_NAME.");
            }
            statement.execute("CREATE SCHEMA " + schema);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create isolated integration test schema.", exception);
        }
        String jdbcUrl = SETTINGS.jdbcUrl() + "?currentSchema=" + schema;
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", SETTINGS::username);
        registry.add("spring.datasource.password", SETTINGS::password);
        registry.add("spring.flyway.schemas", () -> schema);
        registry.add("spring.flyway.default-schema", () -> schema);
        registry.add("spring.flyway.create-schemas", () -> "false");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> schema);
        registry.add("spring.cache.type", () -> "simple");
    }

    public static void dropSchema(String schema) throws Exception {
        if (SETTINGS == null) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(SETTINGS.jdbcUrl(), SETTINGS.username(),
                SETTINGS.password()); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private record Settings(String host, int port, String database, String username, String password) {
        private static Settings fromEnvironment() {
            String host = System.getenv("WMS_TEST_DB_HOST");
            String database = System.getenv("WMS_TEST_DB_NAME");
            String username = System.getenv("WMS_TEST_DB_USERNAME");
            if (blank(host) || blank(database) || blank(username)) {
                return null;
            }
            return new Settings(host, Integer.parseInt(System.getenv().getOrDefault("WMS_TEST_DB_PORT", "5432")),
                    database, username, System.getenv().getOrDefault("WMS_TEST_DB_PASSWORD", ""));
        }

        private String jdbcUrl() {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database;
        }

        private static boolean blank(String value) {
            return value == null || value.isBlank();
        }
    }
}
