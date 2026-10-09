package com.warehousing.wmsapi.product.entity;

import com.warehousing.wmsapi.common.persistence.AuditableEntity;
import com.warehousing.wmsapi.category.entity.ProductCategoryEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_category_id", nullable = false)
    private ProductCategoryEntity category;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "minimum_stock", nullable = false)
    private int minimumStock;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public ProductEntity(ProductCategoryEntity category, String sku, String name, String description,
                         String unit, int minimumStock, boolean active) {
        update(category, sku, name, description, unit, minimumStock, active);
    }

    public void update(ProductCategoryEntity category, String sku, String name, String description,
                       String unit, int minimumStock, boolean active) {
        this.category = category;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.unit = unit;
        this.minimumStock = minimumStock;
        this.active = active;
    }

}
