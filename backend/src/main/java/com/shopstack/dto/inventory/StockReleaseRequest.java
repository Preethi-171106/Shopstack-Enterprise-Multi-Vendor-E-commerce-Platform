package com.shopstack.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * StockReleaseRequest — Request DTO to release reserved stock back to available pool (RELEASED).
 */
public record StockReleaseRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        String referenceId,

        String notes
) {}
