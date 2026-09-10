package com.shopstack.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * StockAddRequest — Request DTO to add stock units (Stock IN).
 */
public record StockAddRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        String referenceId,

        String notes
) {}
