package com.quickserve.modules.inventory.controller;

import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.response.PagedResponse;
import com.quickserve.modules.inventory.dto.InventoryDtos;
import com.quickserve.modules.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER','KITCHEN_STAFF')")
    public ApiResponse<PagedResponse<InventoryDtos.ItemResponse>> listItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(inventoryService.listItems(page, size));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER')")
    public ApiResponse<InventoryDtos.ItemResponse> createItem(
            @Valid @RequestBody InventoryDtos.CreateItemRequest req) {
        return ApiResponse.ok(inventoryService.createItem(req));
    }

    @PostMapping("/items/{itemId}/stock")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER','KITCHEN_STAFF')")
    public ApiResponse<InventoryDtos.ItemResponse> adjustStock(
            @PathVariable UUID itemId,
            @Valid @RequestBody InventoryDtos.UpdateStockRequest req) {
        return ApiResponse.ok(inventoryService.adjustStock(itemId, req));
    }
}
