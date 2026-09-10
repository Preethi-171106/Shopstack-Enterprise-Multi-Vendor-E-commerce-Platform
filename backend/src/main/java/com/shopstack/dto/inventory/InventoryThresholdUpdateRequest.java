package com.shopstack.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * InventoryThresholdUpdateRequest — Request DTO to update the low-stock alert threshold.
 */
public record InventoryThresholdUpdateRequest(
        @NotNull(message = "Low stock threshold is required")
        @Min(value = 0, message = "Low stock threshold must be 0 or greater")
        Integer lowStockThreshold
) {}
