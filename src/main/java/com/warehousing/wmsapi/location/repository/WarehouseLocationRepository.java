package com.warehousing.wmsapi.location.repository;

import com.warehousing.wmsapi.location.entity.WarehouseLocationEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocationEntity, UUID> {
    @EntityGraph(attributePaths = "warehouse")
    Optional<WarehouseLocationEntity> findByIdAndWarehouse_IdAndDeletedAtIsNull(UUID id, UUID warehouseId);

    boolean existsByWarehouse_IdAndCode(UUID warehouseId, String code);
    boolean existsByWarehouse_IdAndDeletedAtIsNull(UUID warehouseId);

    @EntityGraph(attributePaths = "warehouse")
    Page<WarehouseLocationEntity> findAllByWarehouse_IdAndDeletedAtIsNull(UUID warehouseId, Pageable pageable);
}
