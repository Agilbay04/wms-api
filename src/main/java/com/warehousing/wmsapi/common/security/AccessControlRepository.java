package com.warehousing.wmsapi.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccessControlRepository {
    private final JdbcTemplate jdbcTemplate;

    public boolean canAccessWarehouse(String email, java.util.UUID warehouseId) {
        Boolean allowed = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM users u
                    WHERE u.email = ? AND u.deleted_at IS NULL AND u.is_active AND (
                        EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                            WHERE ur.user_id = u.id AND ur.deleted_at IS NULL AND r.deleted_at IS NULL
                              AND r.name = 'SUPERADMIN')
                        OR EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id
                            AND uw.warehouse_id = ? AND uw.deleted_at IS NULL)
                    )
                )
                """, Boolean.class, email, warehouseId);
        return Boolean.TRUE.equals(allowed);
    }

    public boolean hasPermission(String email, String resource, String operation) {
        Boolean allowed = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM users u JOIN user_roles ur ON ur.user_id = u.id
                    JOIN role_permissions rp ON rp.role_id = ur.role_id
                    JOIN permissions p ON p.id = rp.permission_id
                    JOIN resources r ON r.id = p.resource_id
                    JOIN operations o ON o.id = p.operation_id
                    WHERE u.email = ? AND u.deleted_at IS NULL AND u.is_active
                    AND ur.deleted_at IS NULL AND rp.deleted_at IS NULL AND p.deleted_at IS NULL
                    AND r.deleted_at IS NULL AND o.deleted_at IS NULL AND r.name = ? AND o.name = ?
                )
                """, Boolean.class, email, resource, operation);
        return Boolean.TRUE.equals(allowed);
    }
}
