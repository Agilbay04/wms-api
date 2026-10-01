package com.warehousing.wmsapi.category.repository;

import com.warehousing.wmsapi.category.entity.ProductCategoryEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, UUID> {
    Optional<ProductCategoryEntity> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByCode(String code);
    Page<ProductCategoryEntity> findAllByDeletedAtIsNull(Pageable pageable);
}
