package com.quickserve.modules.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

public class InventoryDtos {

    @Data
    public static class CreateItemRequest {
        @NotBlank String name;
        @NotBlank String unit;
        @NotNull @DecimalMin("0") BigDecimal currentStock;
        @NotNull @DecimalMin("0") BigDecimal reorderLevel;
        BigDecimal costPerUnit;
    }

    @Data
    public static class UpdateStockRequest {
        @NotBlank String movementType; // PURCHASE | ADJUSTMENT | CONSUMPTION | WASTE
        @NotNull @DecimalMin("0.001") BigDecimal quantity;
        String notes;
    }

    @Data
    public static class ItemResponse {
        String id;
        String name;
        String unit;
        BigDecimal currentStock;
        BigDecimal reorderLevel;
        BigDecimal costPerUnit;
        boolean active;
        boolean lowStock;  // currentStock <= reorderLevel
    }
}
