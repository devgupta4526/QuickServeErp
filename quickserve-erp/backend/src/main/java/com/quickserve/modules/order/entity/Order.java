package com.quickserve.modules.order.entity;

import com.quickserve.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order extends TenantEntity {

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "order_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderType orderType = OrderType.DINE_IN;

    @Column(name = "table_id")
    private UUID tableId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "staff_id")
    private UUID staffId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.DRAFT;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "service_charge", nullable = false)
    private BigDecimal serviceCharge = BigDecimal.ZERO;

    @Column(name = "total", nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "notes")
    private String notes;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    // items loaded separately via OrderItemRepository to keep aggregates clean
    @Transient
    private java.util.List<OrderItem> items = new java.util.ArrayList<>();

    public enum OrderType { DINE_IN, TAKEAWAY, DELIVERY, QR_SELF }
    public enum OrderStatus { DRAFT, PLACED, PREPARING, READY, DELIVERED, CANCELLED }
    public enum PaymentStatus { PENDING, PARTIAL, PAID }
}
