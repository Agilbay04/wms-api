package com.warehousing.wmsapi.config;

import com.warehousing.wmsapi.iam.repository.UserRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {
    private static final List<String> OPERATIONS = List.of("CREATE", "READ", "UPDATE", "DELETE", "APPROVE", "REJECT", "EXPORT");
    private static final List<String> RESOURCES = List.of("USERS", "ROLES", "PRODUCTS", "PRODUCT_CATEGORIES", "WAREHOUSES", "WAREHOUSE_LOCATIONS", "INVENTORY", "INBOUNDS", "OUTBOUNDS", "STOCK_TRANSFERS", "STOCK_ADJUSTMENTS", "REPORTS");
    private static final List<String> TRANSACTION_RESOURCES = List.of("INBOUNDS", "OUTBOUNDS", "STOCK_TRANSFERS", "STOCK_ADJUSTMENTS");
    private static final List<String> MASTER_READ_RESOURCES = List.of("PRODUCTS", "PRODUCT_CATEGORIES", "WAREHOUSES", "WAREHOUSE_LOCATIONS", "INVENTORY");
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    public DevDataSeeder(JdbcTemplate jdbcTemplate, UserRepository userRepository,
                         PasswordEncoder passwordEncoder, AppProperties appProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        seedReferenceData();
        if (hasSeedAdminConfiguration()) seedSuperadmin();
    }

    private void seedReferenceData() {
        OPERATIONS.forEach(value -> jdbcTemplate.update("INSERT INTO operations(name) VALUES (?) ON CONFLICT (name) DO NOTHING", value));
        RESOURCES.forEach(value -> jdbcTemplate.update("INSERT INTO resources(name) VALUES (?) ON CONFLICT (name) DO NOTHING", value));
        jdbcTemplate.update("INSERT INTO roles(name, description) VALUES ('SUPERADMIN', 'Full system access') ON CONFLICT (name) DO NOTHING");
        jdbcTemplate.update("INSERT INTO roles(name, description) VALUES ('SUPERVISOR', 'Transaction approval and reporting access') ON CONFLICT (name) DO NOTHING");
        jdbcTemplate.update("INSERT INTO roles(name, description) VALUES ('STAFF', 'Warehouse transaction access') ON CONFLICT (name) DO NOTHING");
        jdbcTemplate.update("INSERT INTO permissions(operation_id, resource_id) SELECT o.id, r.id FROM operations o CROSS JOIN resources r ON CONFLICT (operation_id, resource_id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'SUPERADMIN' ON CONFLICT (role_id, permission_id) DO NOTHING");
        grantPermissions("STAFF", TRANSACTION_RESOURCES, List.of("CREATE", "READ", "UPDATE"));
        grantPermissions("STAFF", MASTER_READ_RESOURCES, List.of("READ"));
        grantPermissions("SUPERVISOR", TRANSACTION_RESOURCES, List.of("READ", "APPROVE", "REJECT"));
        grantPermissions("SUPERVISOR", MASTER_READ_RESOURCES, List.of("READ"));
        grantPermissions("SUPERVISOR", List.of("REPORTS"), List.of("READ", "EXPORT"));
    }

    private void seedSuperadmin() {
        AppProperties.SeedAdminProperties admin = appProperties.seedAdmin();
        if (!userRepository.existsByEmail(admin.email())) {
            jdbcTemplate.update("INSERT INTO users(email, name, password, is_active) VALUES (?, ?, ?, true)", admin.email(), admin.name(), passwordEncoder.encode(admin.password()));
        }
        jdbcTemplate.update("INSERT INTO user_roles(user_id, role_id) SELECT u.id, r.id FROM users u CROSS JOIN roles r WHERE u.email = ? AND r.name = 'SUPERADMIN' ON CONFLICT (user_id, role_id) DO NOTHING", admin.email());
    }

    private void grantPermissions(String role, List<String> resources, List<String> operations) {
        for (String resource : resources) {
            for (String operation : operations) {
                jdbcTemplate.update("""
                        INSERT INTO role_permissions(role_id, permission_id)
                        SELECT role.id, permission.id FROM roles role
                        JOIN permissions permission ON true
                        JOIN resources resource ON resource.id = permission.resource_id
                        JOIN operations operation ON operation.id = permission.operation_id
                        WHERE role.name = ? AND resource.name = ? AND operation.name = ?
                        ON CONFLICT (role_id, permission_id) DO NOTHING
                        """, role, resource, operation);
            }
        }
    }

    private boolean hasSeedAdminConfiguration() {
        AppProperties.SeedAdminProperties admin = appProperties.seedAdmin();
        return admin != null && !admin.email().isBlank() && !admin.name().isBlank() && !admin.password().isBlank();
    }
}
