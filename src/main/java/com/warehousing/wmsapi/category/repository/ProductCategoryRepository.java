package com.warehousing.wmsapi.category.repository;

import com.warehousing.wmsapi.category.entity.ProductCategoryEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, UUID> {
    Optional<ProductCategoryEntity> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByCode(String code);
    Page<ProductCategoryEntity> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("""
            select c from ProductCategoryEntity c
            where c.deletedAt is null and (lower(c.code) like lower(concat('%', :search, '%'))
                or lower(c.name) like lower(concat('%', :search, '%'))
                or lower(coalesce(c.description, '')) like lower(concat('%', :search, '%')))
            """)
    Page<ProductCategoryEntity> searchActive(@Param("search") String search, Pageable pageable);
}
