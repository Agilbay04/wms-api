package com.warehousing.wmsapi.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DevSeedRepository {
    private final JdbcTemplate jdbcTemplate;

    public void seedReferenceData(List<String> operations, List<String> resources) {
        operations.forEach(value -> jdbcTemplate.update(
                "INSERT INTO operations(name) VALUES (?) ON CONFLICT (name) DO NOTHING", value));
        resources.forEach(value -> jdbcTemplate.update(
                "INSERT INTO resources(name) VALUES (?) ON CONFLICT (name) DO NOTHING", value));
        jdbcTemplate.update("INSERT INTO roles(name, description) VALUES ('SUPERADMIN', 'Full system access') ON CONFLICT (name) DO NOTHING");
        jdbcTemplate.update("INSERT INTO roles(name, description) VALUES ('SUPERVISOR', 'Transaction approval and reporting access') ON CONFLICT (name) DO NOTHING");
        jdbcTemplate.update("INSERT INTO roles(name, description) VALUES ('STAFF', 'Warehouse transaction access') ON CONFLICT (name) DO NOTHING");
        jdbcTemplate.update("INSERT INTO permissions(operation_id, resource_id) SELECT o.id, r.id FROM operations o CROSS JOIN resources r ON CONFLICT (operation_id, resource_id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'SUPERADMIN' ON CONFLICT (role_id, permission_id) DO NOTHING");
    }

    public void grantPermissions(String role, List<String> resources, List<String> operations) {
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

    public void insertSeedAdmin(String email, String name, String encodedPassword) {
        jdbcTemplate.update("INSERT INTO users(email, name, password, is_active) VALUES (?, ?, ?, true)",
                email, name, encodedPassword);
    }

    public void assignSuperadmin(String email) {
        jdbcTemplate.update("INSERT INTO user_roles(user_id, role_id) SELECT u.id, r.id FROM users u CROSS JOIN roles r WHERE u.email = ? AND r.name = 'SUPERADMIN' ON CONFLICT (user_id, role_id) DO NOTHING", email);
    }
}
