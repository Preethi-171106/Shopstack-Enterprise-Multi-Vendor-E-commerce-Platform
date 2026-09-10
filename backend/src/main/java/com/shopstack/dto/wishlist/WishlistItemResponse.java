package com.shopstack.dto.wishlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WishlistItemResponse — Client representation of a saved product in a user's wishlist.
 */
public record WishlistItemResponse(
        Long id,
        Long productId,
        String productName,
        String productSlug,
        BigDecimal price,
        String imageUrl,
        Long vendorProfileId,
        String storeName,
        BigDecimal rating,
        int reviewCount,
        int stockQuantity,
        boolean active,
        LocalDateTime createdAt
) {
}
