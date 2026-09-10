package com.shopstack.dto.cart;

import java.math.BigDecimal;
import java.util.List;

/**
 * CartResponse — Complete view of a user's shopping cart including item list and total subtotal.
 */
public record CartResponse(
        List<CartItemResponse> items,
        BigDecimal subtotal,
        int totalItems,
        int totalQuantity
) {
}
