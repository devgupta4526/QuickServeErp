package com.quickserve.modules.menu.entity;

import com.quickserve.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "menu_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MenuItem extends TenantEntity {

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "tax_slab_id")
    private UUID taxSlabId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_veg")
    private boolean veg = true;

    @Column(name = "is_available")
    private boolean available = true;

    @Column(name = "is_archived")
    private boolean archived = false;

    @Column(name = "preparation_time")
    private Integer preparationTime; // minutes

    @Column(name = "calories")
    private Integer calories;

    @Column(name = "sort_order")
    private int sortOrder = 0;
}
