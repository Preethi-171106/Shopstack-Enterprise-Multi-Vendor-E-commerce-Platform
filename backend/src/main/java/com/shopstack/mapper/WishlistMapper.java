package com.shopstack.mapper;

import com.shopstack.dto.wishlist.WishlistItemResponse;
import com.shopstack.entity.Product;
import com.shopstack.entity.WishlistItem;
import org.springframework.stereotype.Component;

/**
 * WishlistMapper — Maps {@link WishlistItem} entities to {@link WishlistItemResponse}.
 */
@Component
public class WishlistMapper {

    public WishlistItemResponse toResponse(WishlistItem item) {
        if (item == null) {
            return null;
        }

        Product product = item.getProduct();

        return new WishlistItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getPrice(),
                product.getImageUrl(),
                product.getVendorProfile().getId(),
                product.getVendorProfile().getStoreName(),
                product.getRating(),
                product.getReviewCount(),
                product.getStockQuantity(),
                product.isActive(),
                item.getCreatedAt()
        );
    }
}
