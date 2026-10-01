package com.warehousing.wmsapi.common.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("permissionService")
public class PermissionServiceImpl implements PermissionService {
    private final JdbcTemplate jdbcTemplate;
    public PermissionServiceImpl(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Override
    public boolean hasPermission(Authentication authentication, String resource, String operation) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        Boolean allowed = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN role_permissions rp ON rp.role_id = ur.role_id
                    JOIN permissions p ON p.id = rp.permission_id JOIN resources r ON r.id = p.resource_id JOIN operations o ON o.id = p.operation_id
                    WHERE u.email = ? AND u.deleted_at IS NULL AND u.is_active
                    AND ur.deleted_at IS NULL AND rp.deleted_at IS NULL AND p.deleted_at IS NULL
                    AND r.deleted_at IS NULL AND o.deleted_at IS NULL AND r.name = ? AND o.name = ?
                )
                """, Boolean.class, authentication.getName(), resource, operation);
        return Boolean.TRUE.equals(allowed);
    }
}
