package com.quickserve.modules.order.controller;

import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.response.PagedResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.order.dto.OrderDtos;
import com.quickserve.modules.order.entity.Order;
import com.quickserve.modules.order.entity.Payment;
import com.quickserve.modules.order.repository.OrderRepository;
import com.quickserve.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "POS order management")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService    orderService;
    private final OrderRepository orderRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER','CASHIER','WAITER')")
    public ResponseEntity<ApiResponse<Order>> createOrder(@Valid @RequestBody OrderDtos.CreateOrderRequest req) {
        Order order = orderService.createOrder(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Order created", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Order>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var paged = orderRepository.findByBusinessId(TenantContext.getBusinessId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(paged)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrder(@PathVariable UUID id) {
        Order order = orderRepository.findByIdAndBusinessId(id, TenantContext.getBusinessId())
                .orElseThrow(() -> new com.quickserve.common.exception.ResourceNotFoundException("Order", id));
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER','CASHIER')")
    public ResponseEntity<ApiResponse<Order>> updateStatus(
            @PathVariable UUID id, @RequestBody OrderDtos.StatusUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.updateStatus(id, req.getStatus())));
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER','CASHIER')")
    public ResponseEntity<ApiResponse<Payment>> processPayment(
            @PathVariable UUID id, @Valid @RequestBody OrderDtos.PaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.processPayment(id, req)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER','CASHIER')")
    public ResponseEntity<ApiResponse<Order>> cancelOrder(
            @PathVariable UUID id, @RequestBody OrderDtos.CancelRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.cancelOrder(id, req.getReason())));
    }

    @PostMapping("/{id}/apply-discount")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER')")
    public ResponseEntity<ApiResponse<Order>> applyDiscount(
            @PathVariable UUID id, @RequestBody OrderDtos.DiscountRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.applyDiscount(id, req.getAmount(), req.isPercentage())));
    }
}
