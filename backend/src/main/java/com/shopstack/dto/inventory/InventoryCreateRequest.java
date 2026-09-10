package com.shopstack.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * InventoryCreateRequest — Request DTO to initialize inventory for a product.
 */
public record InventoryCreateRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @Min(value = 0, message = "Initial stock cannot be negative")
        Integer initialStock,

        @Min(value = 0, message = "Low stock threshold cannot be negative")
        Integer lowStockThreshold
) {}
