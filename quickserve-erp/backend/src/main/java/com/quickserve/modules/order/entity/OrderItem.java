package com.quickserve.modules.order.entity;

import com.quickserve.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "menu_item_id")
    private UUID menuItemId;

    @Column(name = "menu_item_name", nullable = false)
    private String menuItemName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "addons", columnDefinition = "jsonb")
    private String addons = "[]";

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "kds_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private KdsStatus kdsStatus = KdsStatus.PENDING;

    @Column(name = "kds_notes")
    private String kdsNotes;

    public enum KdsStatus { PENDING, PREPARING, DONE }
}
