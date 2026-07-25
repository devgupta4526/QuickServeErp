package com.quickserve.modules.inventory.service;

import com.quickserve.common.events.EventPublisher;
import com.quickserve.common.exception.ResourceNotFoundException;
import com.quickserve.common.response.PagedResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.inventory.dto.InventoryDtos;
import com.quickserve.modules.inventory.entity.InventoryItem;
import com.quickserve.modules.inventory.entity.InventoryMovement;
import com.quickserve.modules.inventory.repository.InventoryItemRepository;
import com.quickserve.modules.inventory.repository.InventoryMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryMovementRepository movementRepository;
    private final EventPublisher eventPublisher;

    // ── List items ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<InventoryDtos.ItemResponse> listItems(int page, int size) {
        UUID businessId = TenantContext.getBusinessId();
        Page<InventoryItem> result = itemRepository.findByBusinessId(
                businessId, PageRequest.of(page, size, Sort.by("name")));
        return PagedResponse.of(result, this::toResponse);
    }

    // ── Create item ────────────────────────────────────────────────────────────

    @Transactional
    public InventoryDtos.ItemResponse createItem(InventoryDtos.CreateItemRequest req) {
        UUID businessId = TenantContext.getBusinessId();
        InventoryItem item = InventoryItem.builder()
                .name(req.getName())
                .unit(req.getUnit())
                .currentStock(req.getCurrentStock())
                .reorderLevel(req.getReorderLevel())
                .costPerUnit(req.getCostPerUnit())
                .active(true)
                .build();
        item.setBusinessId(businessId);
        return toResponse(itemRepository.save(item));
    }

    // ── Adjust stock ───────────────────────────────────────────────────────────

    @Transactional
    public InventoryDtos.ItemResponse adjustStock(UUID itemId, InventoryDtos.UpdateStockRequest req) {
        UUID businessId = TenantContext.getBusinessId();
        InventoryItem item = itemRepository.findById(itemId)
                .filter(i -> i.getBusinessId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", itemId.toString()));

        InventoryMovement.MovementType type =
                InventoryMovement.MovementType.valueOf(req.getMovementType().toUpperCase());

        // Apply movement
        switch (type) {
            case PURCHASE, ADJUSTMENT -> item.setCurrentStock(item.getCurrentStock().add(req.getQuantity()));
            case CONSUMPTION, WASTE   -> item.setCurrentStock(item.getCurrentStock().subtract(req.getQuantity()));
        }

        // Record movement
        InventoryMovement movement = InventoryMovement.builder()
                .item(item)
                .movementType(type)
                .quantity(req.getQuantity())
                .notes(req.getNotes())
                .build();
        movement.setBusinessId(businessId);
        movementRepository.save(movement);

        // Publish low-stock event if threshold breached
        if (item.getCurrentStock().compareTo(item.getReorderLevel()) <= 0) {
            log.warn("LOW STOCK ALERT: {} — current={} unit={}",
                    item.getName(), item.getCurrentStock(), item.getUnit());
            eventPublisher.publish("stock.low", itemId.toString());
        }

        return toResponse(itemRepository.save(item));
    }

    // ── Deduct stock (called from OrderService) ────────────────────────────────

    @Transactional
    public void deductForOrder(UUID itemId, java.math.BigDecimal qty, String notes) {
        InventoryDtos.UpdateStockRequest req = new InventoryDtos.UpdateStockRequest();
        req.setMovementType("CONSUMPTION");
        req.setQuantity(qty);
        req.setNotes(notes);
        adjustStock(itemId, req);
    }

    // ── Mapping ────────────────────────────────────────────────────────────────

    private InventoryDtos.ItemResponse toResponse(InventoryItem item) {
        InventoryDtos.ItemResponse r = new InventoryDtos.ItemResponse();
        r.setId(item.getId().toString());
        r.setName(item.getName());
        r.setUnit(item.getUnit());
        r.setCurrentStock(item.getCurrentStock());
        r.setReorderLevel(item.getReorderLevel());
        r.setCostPerUnit(item.getCostPerUnit());
        r.setActive(item.isActive());
        r.setLowStock(item.getCurrentStock().compareTo(item.getReorderLevel()) <= 0);
        return r;
    }
}
