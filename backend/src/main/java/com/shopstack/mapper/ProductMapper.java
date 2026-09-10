package com.shopstack.mapper;

import com.shopstack.dto.product.ProductResponse;
import com.shopstack.entity.Product;
import org.springframework.stereotype.Component;

/**
 * ProductMapper — Component mapping {@link Product} entities to {@link ProductResponse} DTOs.
 *
 * <p>Follows the same lightweight pattern as {@link CategoryMapper} and {@link VendorProfileMapper}.
 * No framework (MapStruct, ModelMapper) is used — just a plain Spring component.
 *
 * <p>The mapping is deliberately flat:
 * <ul>
 *   <li>Vendor store name is pulled from {@code product.vendorProfile.storeName}</li>
 *   <li>Category name and slug are pulled from {@code product.category.name/slug}</li>
 *   <li>No internal entity object is ever exposed to the client</li>
 * </ul>
 */
@Component
public class ProductMapper {

    /**
     * Converts a {@link Product} entity to a {@link ProductResponse} DTO.
     *
     * <p>Requires that {@code product.vendorProfile} and {@code product.category}
     * are already loaded (not in lazy-loading proxy state). The service layer is
     * responsible for ensuring associations are initialized before calling this method.
     *
     * @param product the persistent product entity
     * @return {@link ProductResponse} containing client-safe fields, or {@code null} if input is null
     */
    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        return new ProductResponse(
                product.getId(),
                product.getVendorProfile().getId(),
                product.getVendorProfile().getStoreName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStockQuantity(),
                product.getSku(),
                product.getImageUrl(),
                product.isActive(),
                product.isFeatured(),
                product.getRating(),
                product.getReviewCount(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
