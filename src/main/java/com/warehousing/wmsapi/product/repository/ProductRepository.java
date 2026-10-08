package com.warehousing.wmsapi.product.repository;

import com.warehousing.wmsapi.product.entity.ProductEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsBySku(String sku);

    boolean existsByCategory_IdAndDeletedAtIsNull(UUID categoryId);

    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findAllByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from ProductEntity p
            where p.deletedAt is null and (lower(p.sku) like lower(concat('%', :search, '%'))
                or lower(p.name) like lower(concat('%', :search, '%'))
                or lower(coalesce(p.description, '')) like lower(concat('%', :search, '%'))
                or lower(p.unit) like lower(concat('%', :search, '%')))
            """)
    Page<ProductEntity> searchActive(@Param("search") String search, Pageable pageable);
}
