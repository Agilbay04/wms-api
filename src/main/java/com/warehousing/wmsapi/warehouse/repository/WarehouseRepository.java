package com.warehousing.wmsapi.warehouse.repository;

import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<WarehouseEntity, UUID> {
    Optional<WarehouseEntity> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByCode(String code);

}
