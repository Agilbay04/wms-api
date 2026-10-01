package com.warehousing.wmsapi.product.repository;

import com.warehousing.wmsapi.product.entity.ProductEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsBySku(String sku);

    boolean existsByCategory_IdAndDeletedAtIsNull(UUID categoryId);

    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findAllByDeletedAtIsNull(Pageable pageable);
}
