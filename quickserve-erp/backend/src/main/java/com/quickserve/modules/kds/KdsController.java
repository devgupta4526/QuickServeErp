package com.quickserve.modules.kds;

import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.order.entity.Order;
import com.quickserve.modules.order.entity.OrderItem;
import com.quickserve.modules.order.repository.OrderItemRepository;
import com.quickserve.modules.order.repository.OrderRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/kds")
@Tag(name = "KDS", description = "Kitchen Display System — real-time order management")
@RequiredArgsConstructor
public class KdsController {

    private final OrderRepository     orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/display/{outletId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER','KITCHEN_STAFF')")
    public ResponseEntity<ApiResponse<List<Order>>> getActiveOrders(@PathVariable UUID outletId) {
        List<Order> orders = orderRepository.findByOutletIdAndStatusIn(outletId,
                List.of(Order.OrderStatus.PLACED, Order.OrderStatus.PREPARING));
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @PatchMapping("/items/{id}/status")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER','KITCHEN_STAFF')")
    public ResponseEntity<ApiResponse<OrderItem>> updateItemStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        OrderItem.KdsStatus newStatus = OrderItem.KdsStatus.valueOf(body.get("status"));
        OrderItem item = orderItemRepository.findById(id)
                .orElseThrow(() -> new com.quickserve.common.exception.ResourceNotFoundException("OrderItem", id));
        item.setKdsStatus(newStatus);
        item = orderItemRepository.save(item);

        // Broadcast to WebSocket subscribers
        messagingTemplate.convertAndSend(
                "/topic/kds/" + item.getOrderId(),
                Map.of("itemId", item.getId(), "status", newStatus, "orderItemName", item.getMenuItemName())
        );

        return ResponseEntity.ok(ApiResponse.ok("Status updated", item));
    }
}
