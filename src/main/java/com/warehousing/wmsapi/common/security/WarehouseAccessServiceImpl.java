package com.warehousing.wmsapi.common.security;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("warehouseAccessService")
public class WarehouseAccessServiceImpl implements WarehouseAccessService {
    private final JdbcTemplate jdbcTemplate;
    public WarehouseAccessServiceImpl(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Override
    public boolean canAccessWarehouse(Authentication authentication, UUID warehouseId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        Boolean allowed = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM users u
                    WHERE u.email = ? AND u.deleted_at IS NULL AND u.is_active AND (
                        EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = u.id AND ur.deleted_at IS NULL AND r.deleted_at IS NULL AND r.name = 'SUPERADMIN')
                        OR EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id AND uw.warehouse_id = ? AND uw.deleted_at IS NULL)
                    )
                )
                """, Boolean.class, authentication.getName(), warehouseId);
        return Boolean.TRUE.equals(allowed);
    }
}
