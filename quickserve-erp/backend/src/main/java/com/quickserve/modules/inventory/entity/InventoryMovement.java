package com.quickserve.modules.inventory.entity;

import com.quickserve.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Single movement: PURCHASE, ADJUSTMENT, CONSUMPTION, WASTE */
@Entity
@Table(name = "inventory_movements",
       indexes = @Index(name = "idx_inv_mv_item", columnList = "item_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryMovement extends TenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType movementType;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(length = 200)
    private String notes;

    private LocalDateTime movedAt = LocalDateTime.now();

    public enum MovementType { PURCHASE, ADJUSTMENT, CONSUMPTION, WASTE }
}
