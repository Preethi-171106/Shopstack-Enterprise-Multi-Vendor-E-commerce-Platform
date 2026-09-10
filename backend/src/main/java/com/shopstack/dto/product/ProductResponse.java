package com.shopstack.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ProductResponse — DTO returned from all Product API endpoints.
 *
 * <p>This record is the client-safe view of a product. It includes:
 * <ul>
 *   <li>Product fields visible to all callers (public, vendor, admin)</li>
 *   <li>Vendor store name (for public browsing context)</li>
 *   <li>Category name and slug (for display and navigation)</li>
 * </ul>
 *
 * <p>Intentionally excluded from client responses:
 * <ul>
 *   <li>vendorProfile internal ID (not client-relevant)</li>
 *   <li>vendorProfile.user (internal user entity)</li>
 *   <li>Internal entity relationships</li>
 * </ul>
 *
 * <p>Uses Java {@code record} for immutability and compact syntax,
 * consistent with the existing DTO pattern in this project.
 */
public record ProductResponse(
        Long id,
        Long vendorProfileId,
        String storeName,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String name,
        String slug,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        int stockQuantity,
        String sku,
        String imageUrl,
        boolean active,
        boolean featured,
        BigDecimal rating,
        int reviewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
