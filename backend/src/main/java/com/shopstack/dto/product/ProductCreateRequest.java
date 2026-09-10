package com.shopstack.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

/**
 * ProductCreateRequest — DTO for the Vendor Create Product API.
 *
 * <p>The vendor's identity (vendorProfileId) is intentionally NOT a field here.
 * It is always resolved from the authenticated JWT token in the service layer.
 * This prevents a vendor from falsely claiming ownership of another vendor's profile.
 *
 * <p>Endpoint: {@code POST /api/vendors/products}
 *
 * <p>Fields the vendor provides:
 * <ul>
 *   <li>name — required</li>
 *   <li>categoryId — required (must be a valid, active category)</li>
 *   <li>price — required, non-negative</li>
 *   <li>sku — required, unique</li>
 *   <li>description — optional</li>
 *   <li>originalPrice — optional, should not be less than price if provided</li>
 *   <li>stockQuantity — optional, defaults to 0</li>
 *   <li>imageUrl — optional, must be valid URL</li>
 * </ul>
 */
public record ProductCreateRequest(

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
