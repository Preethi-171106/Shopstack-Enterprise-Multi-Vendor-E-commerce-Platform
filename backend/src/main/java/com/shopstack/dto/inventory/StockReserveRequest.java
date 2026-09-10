package com.shopstack.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * StockReserveRequest — Request DTO to reserve stock for an order (RESERVED).
 */
public record StockReserveRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        String referenceId,

        String notes
) {}
