package com.shopstack.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * CategoryCreateRequest — Input DTO for creating a new product category.
 *
 * <p>The backend auto-generates the slug from the name. The active flag is always set to
 * {@code true} on creation. Clients must not supply id, slug, active, createdAt, or updatedAt.
 */
public record CategoryCreateRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name cannot exceed 100 characters")
        String name,

        @Size(max = 1000, message = "Category description cannot exceed 1000 characters")
        String description,

        @Pattern(
                regexp = "^(https?://.*)?$",
                message = "Image URL must be a valid HTTP or HTTPS URL"
        )
        String imageUrl,

        Long parentCategoryId
) {
    public CategoryCreateRequest(String name, String description, String imageUrl) {
        this(name, description, imageUrl, null);
    }
}
