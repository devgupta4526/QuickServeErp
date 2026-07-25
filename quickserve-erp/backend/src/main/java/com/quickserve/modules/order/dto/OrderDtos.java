package com.quickserve.modules.order.dto;

import com.quickserve.modules.order.entity.Order;
import com.quickserve.modules.order.entity.Payment;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderDtos {

    @Data
    public static class CreateOrderRequest {
        @NotNull private UUID outletId;
        private Order.OrderType orderType;
        private UUID tableId;
        private UUID customerId;
        private String notes;
        @NotEmpty private List<OrderItemRequest> items;
    }

    @Data
    public static class OrderItemRequest {
        @NotNull private UUID menuItemId;
        @Positive private int quantity = 1;
        private UUID variantId;
        private List<UUID> addonIds;
        private String notes;
    }

    @Data
    public static class PaymentRequest {
        @NotNull private BigDecimal amount;
        @NotNull private Payment.PaymentMethod method;
        private String transactionId;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String razorpaySignature;
    }

    @Data
    public static class DiscountRequest {
        @Positive private BigDecimal amount;
        private boolean isPercentage = false;
    }

    @Data
    public static class CancelRequest {
        private String reason;
    }

    @Data
    public static class StatusUpdateRequest {
        @NotNull private Order.OrderStatus status;
    }
}
