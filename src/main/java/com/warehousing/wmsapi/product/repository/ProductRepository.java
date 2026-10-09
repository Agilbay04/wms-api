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

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM inbound_items WHERE product_id = :id AND deleted_at IS NULL
                UNION ALL SELECT 1 FROM outbound_items WHERE product_id = :id AND deleted_at IS NULL
                UNION ALL SELECT 1 FROM stock_transfer_items WHERE product_id = :id AND deleted_at IS NULL
                UNION ALL SELECT 1 FROM stock_adjustment_items WHERE product_id = :id AND deleted_at IS NULL
                UNION ALL SELECT 1 FROM warehouse_location_items WHERE product_id = :id AND deleted_at IS NULL
                UNION ALL SELECT 1 FROM stock_movements WHERE product_id = :id AND deleted_at IS NULL
            )
            """, nativeQuery = true)
    boolean isReferenced(@Param("id") UUID id);

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
