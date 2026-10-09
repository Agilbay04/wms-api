package com.warehousing.wmsapi.location.repository;

import com.warehousing.wmsapi.location.entity.WarehouseLocationEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocationEntity, UUID> {
    @EntityGraph(attributePaths = "warehouse")
    Optional<WarehouseLocationEntity> findByIdAndWarehouse_IdAndDeletedAtIsNull(UUID id, UUID warehouseId);

    boolean existsByWarehouse_IdAndCode(UUID warehouseId, String code);
    boolean existsByWarehouse_IdAndDeletedAtIsNull(UUID warehouseId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM warehouse_location_items WHERE warehouse_location_id = :id AND deleted_at IS NULL
                UNION ALL SELECT 1 FROM stock_movements WHERE warehouse_location_id = :id AND deleted_at IS NULL
                UNION ALL SELECT 1 FROM stock_transfer_items WHERE source_warehouse_location_id = :id AND deleted_at IS NULL
                UNION ALL SELECT 1 FROM stock_transfer_items WHERE destination_warehouse_location_id = :id AND deleted_at IS NULL
                UNION ALL SELECT 1 FROM stock_adjustment_items WHERE warehouse_location_id = :id AND deleted_at IS NULL
                UNION ALL SELECT 1 FROM outbound_items WHERE warehouse_location_id = :id AND deleted_at IS NULL
            )
            """, nativeQuery = true)
    boolean isReferenced(@Param("id") UUID id);

    @EntityGraph(attributePaths = "warehouse")
    Page<WarehouseLocationEntity> findAllByWarehouse_IdAndDeletedAtIsNull(UUID warehouseId, Pageable pageable);

    @EntityGraph(attributePaths = "warehouse")
    @Query("""
            select l from WarehouseLocationEntity l
            where l.warehouse.id = :warehouseId and l.deletedAt is null and (lower(l.code) like lower(concat('%', :search, '%'))
                or lower(l.name) like lower(concat('%', :search, '%'))
                or lower(coalesce(l.description, '')) like lower(concat('%', :search, '%')))
            """)
    Page<WarehouseLocationEntity> searchActiveByWarehouse(@Param("warehouseId") UUID warehouseId,
            @Param("search") String search, Pageable pageable);
}
