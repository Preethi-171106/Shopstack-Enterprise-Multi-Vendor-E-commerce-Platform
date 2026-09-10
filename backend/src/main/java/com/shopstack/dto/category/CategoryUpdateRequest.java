package com.shopstack.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * CategoryUpdateRequest — Input DTO for updating an existing product category.
 *
 * <p>Clients can update name, description, and imageUrl only.
 * The slug is auto-regenerated from the new name if the name changes.
 * Clients must NOT supply id, slug, active, createdAt, or updatedAt — those fields are ignored.
 */
public record CategoryUpdateRequest(

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
    public CategoryUpdateRequest(String name, String description, String imageUrl) {
        this(name, description, imageUrl, null);
    }
}
