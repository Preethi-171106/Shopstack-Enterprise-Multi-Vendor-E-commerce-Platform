package com.shopstack.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * StockReturnRequest — Request DTO to record returned customer stock (RETURN).
 */
public record StockReturnRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        String referenceId,

        String notes
) {}
