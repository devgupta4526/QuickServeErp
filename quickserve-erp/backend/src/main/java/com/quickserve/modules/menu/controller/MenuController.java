package com.quickserve.modules.menu.controller;

import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.response.PagedResponse;
import com.quickserve.modules.menu.entity.Category;
import com.quickserve.modules.menu.entity.MenuItem;
import com.quickserve.modules.menu.entity.TaxSlab;
import com.quickserve.modules.menu.service.MenuService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu")
@Tag(name = "Menu", description = "Menu categories and items management")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // ===== CATEGORIES =====

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Category>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getCategories()));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER')")
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody CategoryRequest req) {
        Category cat = menuService.createCategory(req.getName(), req.getImageUrl(), req.getSortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Category created", cat));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER')")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable UUID id, @RequestBody CategoryRequest req) {
        Category cat = menuService.updateCategory(id, req.getName(), req.getImageUrl(), req.getSortOrder());
        return ResponseEntity.ok(ApiResponse.ok("Category updated", cat));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        menuService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted", null));
    }

    // ===== MENU ITEMS =====

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<PagedResponse<MenuItem>>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getMenuItems(page, size)));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER')")
    public ResponseEntity<ApiResponse<MenuItem>> createItem(@RequestBody MenuItemRequest req) {
        MenuItem item = MenuItem.builder()
                .name(req.getName())
                .description(req.getDescription())
                .basePrice(req.getBasePrice())
                .categoryId(req.getCategoryId())
                .taxSlabId(req.getTaxSlabId())
                .veg(req.isVeg())
                .available(true)
                .archived(false)
                .preparationTime(req.getPreparationTime())
                .calories(req.getCalories())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Item created", menuService.createMenuItem(item)));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER')")
    public ResponseEntity<ApiResponse<MenuItem>> updateItem(
            @PathVariable UUID id, @RequestBody MenuItemRequest req) {
        MenuItem updates = MenuItem.builder()
                .name(req.getName())
                .description(req.getDescription())
                .basePrice(req.getBasePrice())
                .categoryId(req.getCategoryId())
                .taxSlabId(req.getTaxSlabId())
                .veg(req.isVeg())
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Item updated", menuService.updateMenuItem(id, updates)));
    }

    @PatchMapping("/items/{id}/availability")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER','CASHIER')")
    public ResponseEntity<ApiResponse<MenuItem>> toggleAvailability(
            @PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.toggleAvailability(id, body.get("available"))));
    }

    @GetMapping("/tax-slabs")
    public ResponseEntity<ApiResponse<List<TaxSlab>>> getTaxSlabs() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getTaxSlabs()));
    }

    // ===== PUBLIC (no auth) =====

    @GetMapping("/public/{outletId}")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getPublicMenu(@PathVariable UUID outletId) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getPublicMenu(outletId)));
    }

    // ===== Request DTOs =====

    @Data
    static class CategoryRequest {
        @NotBlank private String name;
        private String imageUrl;
        private int sortOrder = 0;
    }

    @Data
    static class MenuItemRequest {
        @NotBlank private String name;
        private String description;
        @NotNull @Positive private BigDecimal basePrice;
        @NotNull private UUID categoryId;
        private UUID taxSlabId;
        private boolean veg = true;
        private Integer preparationTime;
        private Integer calories;
    }
}
