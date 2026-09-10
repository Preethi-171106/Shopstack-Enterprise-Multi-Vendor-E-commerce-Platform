package com.shopstack.dto.cart;

import java.math.BigDecimal;

/**
 * CartItemResponse — Client representation of an item in the shopping cart.
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String productSlug,
        BigDecimal price,
        String imageUrl,
        Long vendorProfileId,
        String storeName,
        int quantity,
        BigDecimal subtotal,
        boolean active,
        int stockQuantity
) {
}
