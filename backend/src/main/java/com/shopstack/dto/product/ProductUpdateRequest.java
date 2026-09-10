package com.shopstack.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

/**
 * ProductUpdateRequest — DTO for the Vendor/Admin Update Product API.
 *
 * <p>Used with: {@code PUT /api/vendors/products/{id}} and {@code PUT /api/admin/products/{id}}
 *
 * <p>All updatable fields must be provided (full replace semantics).
 * Fields that cannot be updated via this request:
 * <ul>
 *   <li>vendorProfileId — always from JWT, not from client</li>
 *   <li>slug — auto-regenerated from name</li>
 *   <li>rating / reviewCount — managed by review system, not this endpoint</li>
 *   <li>id, createdAt, updatedAt — managed by JPA</li>
 * </ul>
 */
public record ProductUpdateRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Product name must not exceed 200 characters")
        String name,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price must be zero or greater")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "Original price must be zero or greater")
        BigDecimal originalPrice,

        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        String sku,

        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @URL(message = "Image URL must be a valid URL")
        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl
) {
}
