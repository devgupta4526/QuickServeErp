package com.quickserve.modules.inventory.entity;

import com.quickserve.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents a raw material / consumable tracked in inventory.
 * business_id is inherited from TenantEntity (row-level tenant isolation).
 */
@Entity
@Table(name = "inventory_items",
       indexes = @Index(name = "idx_inv_item_business", columnList = "business_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItem extends TenantEntity {

    @Column(nullable = false, length = 120)
    private String name;

    /** Unit of measurement: KG, LTR, UNIT, PKT, DOZEN */
    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal costPerUnit;

    @Column(nullable = false)
    private boolean active = true;
}
