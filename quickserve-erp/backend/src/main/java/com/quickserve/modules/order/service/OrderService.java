package com.quickserve.modules.order.service;

import com.quickserve.common.exception.BusinessException;
import com.quickserve.common.exception.ResourceNotFoundException;
import com.quickserve.common.exception.TenantAccessException;
import com.quickserve.common.security.TenantContext;
import com.quickserve.common.util.GstCalculator;
import com.quickserve.modules.menu.entity.MenuItem;
import com.quickserve.modules.menu.entity.TaxSlab;
import com.quickserve.modules.menu.repository.MenuItemRepository;
import com.quickserve.modules.menu.repository.TaxSlabRepository;
import com.quickserve.modules.order.dto.OrderDtos;
import com.quickserve.modules.order.entity.Order;
import com.quickserve.modules.order.entity.OrderItem;
import com.quickserve.modules.order.entity.Payment;
import com.quickserve.modules.order.repository.OrderRepository;
import com.quickserve.modules.order.repository.PaymentRepository;
import com.quickserve.common.events.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository     orderRepository;
    private final PaymentRepository   paymentRepository;
    private final MenuItemRepository  menuItemRepository;
    private final TaxSlabRepository   taxSlabRepository;
    private final GstCalculator       gstCalculator;
    private final EventPublisher      eventPublisher;

    // ========== CREATE ORDER ==========

    @Transactional
    public Order createOrder(OrderDtos.CreateOrderRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException("Order must have at least one item");
        }

        UUID businessId = TenantContext.getBusinessId();
        String orderNum = generateOrderNumber(req.getOutletId());

        Order order = Order.builder()
                .orderNumber(orderNum)
                .orderType(req.getOrderType() != null ? req.getOrderType() : Order.OrderType.DINE_IN)
                .tableId(req.getTableId())
                .customerId(req.getCustomerId())
                .staffId(TenantContext.getUserId())
                .status(Order.OrderStatus.DRAFT)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .notes(req.getNotes())
                .build();
        order.setBusinessId(businessId);
        order.setOutletId(req.getOutletId());
        order = orderRepository.save(order);

        // Build order items and calculate totals
        BigDecimal subtotal  = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        for (OrderDtos.OrderItemRequest itemReq : req.getItems()) {
            if (itemReq.getQuantity() <= 0) {
                throw new BusinessException("Item quantity must be greater than 0");
            }
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemReq.getMenuItemId()));
            if (!menuItem.isAvailable()) {
                throw new BusinessException("Item '" + menuItem.getName() + "' is not available");
            }

            BigDecimal unitPrice = menuItem.getBasePrice();
            // Add variant price modifier if any
            // (variant loading omitted for brevity — add when VariantRepository is injected)

            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal itemTax      = BigDecimal.ZERO;

            // Calculate GST
            if (menuItem.getTaxSlabId() != null) {
                TaxSlab slab = taxSlabRepository.findById(menuItem.getTaxSlabId()).orElse(null);
                if (slab != null && slab.getPercentage().compareTo(BigDecimal.ZERO) > 0) {
                    itemTax = gstCalculator.calculateGstExclusive(itemSubtotal, slab.getPercentage());
                }
            }

            OrderItem orderItem = OrderItem.builder()
                    .orderId(order.getId())
                    .menuItemId(menuItem.getId())
                    .menuItemName(menuItem.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .taxAmount(itemTax)
                    .totalPrice(itemSubtotal.add(itemTax))
                    .kdsStatus(OrderItem.KdsStatus.PENDING)
                    .build();

            subtotal  = subtotal.add(itemSubtotal);
            taxAmount = taxAmount.add(itemTax);
            order.getItems().add(orderItem);
        }

        order.setSubtotal(subtotal);
        order.setTaxAmount(taxAmount);
        order.setTotal(subtotal.add(taxAmount));
        order.setStatus(Order.OrderStatus.PLACED);
        order = orderRepository.save(order);

        // Publish Kafka event
        eventPublisher.publish("order.placed", order.getId().toString(), Map.of(
                "orderId", order.getId(),
                "businessId", businessId,
                "outletId", order.getOutletId(),
                "total", order.getTotal()
        ));
        eventPublisher.publish("order.status.changed", order.getId().toString(), Map.of(
                "orderId", order.getId(),
                "businessId", businessId,
                "customerId", order.getCustomerId() != null ? order.getCustomerId().toString() : "",
                "newStatus", "PLACED",
                "orderNumber", order.getOrderNumber(),
                "outletName", "" // TODO: resolve outlet name
        ));

        return order;
    }

    // ========== PAYMENT ==========

    @Transactional
    public Payment processPayment(UUID orderId, OrderDtos.PaymentRequest req) {
        Order order = orderRepository.findByIdAndBusinessId(orderId, TenantContext.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BusinessException("Cannot process payment for a cancelled order");
        }

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(req.getAmount())
                .method(req.getMethod())
                .transactionId(req.getTransactionId())
                .razorpayOrderId(req.getRazorpayOrderId())
                .razorpayPaymentId(req.getRazorpayPaymentId())
                .status(Payment.PaymentStatus.SUCCESS)
                .paidAt(Instant.now())
                .build();
        payment = paymentRepository.save(payment);

        // Determine if order is fully or partially paid
        BigDecimal totalPaid = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(order.getTotal()) >= 0) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        } else {
            order.setPaymentStatus(Order.PaymentStatus.PARTIAL);
        }
        orderRepository.save(order);

        // Publish payment event
        eventPublisher.publish("payment.processed", orderId.toString(), Map.of(
                "orderId", orderId,
                "businessId", TenantContext.getBusinessId(),
                "amount", req.getAmount(),
                "method", req.getMethod()
        ));

        return payment;
    }

    // ========== CANCEL ==========

    @Transactional
    public Order cancelOrder(UUID orderId, String reason) {
        Order order = orderRepository.findByIdAndBusinessId(orderId, TenantContext.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new BusinessException("Cannot cancel a delivered order", HttpStatus.CONFLICT);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        return orderRepository.save(order);
    }

    // ========== STATUS UPDATE ==========

    @Transactional
    public Order updateStatus(UUID orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findByIdAndBusinessId(orderId, TenantContext.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setStatus(newStatus);
        order = orderRepository.save(order);

        eventPublisher.publish("order.status.changed", orderId.toString(), Map.of(
                "orderId", orderId,
                "businessId", TenantContext.getBusinessId(),
                "customerId", order.getCustomerId() != null ? order.getCustomerId().toString() : "",
                "newStatus", newStatus.name(),
                "orderNumber", order.getOrderNumber()
        ));

        return order;
    }

    // ========== DISCOUNT ==========

    @Transactional
    public Order applyDiscount(UUID orderId, BigDecimal discountAmount, boolean isPercentage) {
        Order order = orderRepository.findByIdAndBusinessId(orderId, TenantContext.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        BigDecimal discount = isPercentage
                ? order.getSubtotal().multiply(discountAmount).divide(BigDecimal.valueOf(100))
                : discountAmount;

        if (discount.compareTo(order.getTotal()) > 0) {
            throw new BusinessException("Discount cannot exceed order total");
        }

        order.setDiscountAmount(discount);
        order.setTotal(order.getSubtotal().add(order.getTaxAmount()).subtract(discount).add(order.getServiceCharge()));
        return orderRepository.save(order);
    }

    // ========== HELPERS ==========

    private String generateOrderNumber(UUID outletId) {
        // Simple sequential number — in production use DB sequence
        return "ORD-" + System.currentTimeMillis() % 100000;
    }
}
