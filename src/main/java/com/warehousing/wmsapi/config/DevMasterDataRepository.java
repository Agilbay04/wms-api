package com.warehousing.wmsapi.config;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DevMasterDataRepository {
    private final JdbcTemplate jdbcTemplate;

    public SeededId category(String code, String name, String description, boolean active) {
        List<UUID> ids = jdbcTemplate.query("""
                INSERT INTO product_categories(code, name, description, is_active)
                VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING RETURNING id
                """, (row, index) -> row.getObject("id", UUID.class), code, name, description, active);
        return createdOrExisting(ids, "SELECT id FROM product_categories WHERE code = ? AND deleted_at IS NULL AND is_active",
                code, "category");
    }

    public SeededId warehouse(String code, String name, String address, boolean active) {
        List<UUID> ids = jdbcTemplate.query("""
                INSERT INTO warehouses(code, name, address, is_active)
                VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING RETURNING id
                """, (row, index) -> row.getObject("id", UUID.class), code, name, address, active);
        return createdOrExisting(ids, "SELECT id FROM warehouses WHERE code = ? AND deleted_at IS NULL AND is_active",
                code, "warehouse");
    }

    public SeededId product(UUID categoryId, String sku, String name, String description,
                            String unit, int minimumStock, boolean active) {
        List<UUID> ids = jdbcTemplate.query("""
                INSERT INTO products(product_category_id, sku, name, description, unit, minimum_stock, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (sku) DO NOTHING RETURNING id
                """, (row, index) -> row.getObject("id", UUID.class),
                categoryId, sku, name, description, unit, minimumStock, active);
        return new SeededId(ids.isEmpty() ? null : ids.get(0), !ids.isEmpty());
    }

    public SeededId location(UUID warehouseId, String code, String name, String description, boolean active) {
        List<UUID> ids = jdbcTemplate.query("""
                INSERT INTO warehouse_locations(warehouse_id, code, name, description, is_active)
                VALUES (?, ?, ?, ?, ?) ON CONFLICT (warehouse_id, code) DO NOTHING RETURNING id
                """, (row, index) -> row.getObject("id", UUID.class),
                warehouseId, code, name, description, active);
        return new SeededId(ids.isEmpty() ? null : ids.get(0), !ids.isEmpty());
    }

    private SeededId createdOrExisting(List<UUID> inserted, String activeRecordQuery, String code, String kind) {
        if (!inserted.isEmpty()) return new SeededId(inserted.get(0), true);
        try {
            return new SeededId(jdbcTemplate.queryForObject(activeRecordQuery, UUID.class, code), false);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalStateException("Seed " + kind + " " + code + " already exists but is inactive or deleted.",
                    exception);
        }
    }

    public record SeededId(UUID id, boolean created) { }
}
