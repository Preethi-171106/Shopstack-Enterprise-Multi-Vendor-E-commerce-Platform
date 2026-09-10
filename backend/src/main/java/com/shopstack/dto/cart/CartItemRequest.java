package com.shopstack.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * CartItemRequest — DTO for adding an item to the shopping cart.
 */
public record CartItemRequest(

        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
    public CartItemRequest {
        if (quantity == null) {
            quantity = 1;
        }
    }
}
