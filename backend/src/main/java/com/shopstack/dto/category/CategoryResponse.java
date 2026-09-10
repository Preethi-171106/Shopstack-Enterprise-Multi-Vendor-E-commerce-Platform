package com.shopstack.dto.category;

import java.time.LocalDateTime;

/**
 * CategoryResponse — Output DTO returned from all category endpoints.
 *
 * <p>Represents a safe, client-facing view of the {@link com.shopstack.entity.Category} entity.
 * Never exposes internal JPA or database details.
 *
 * @param id          unique database identifier
 * @param name        human-readable category name
 * @param slug        URL-safe identifier auto-generated from name
 * @param description optional description of the category
 * @param imageUrl    optional URL of the category image
 * @param active      whether the category is publicly visible
 * @param createdAt   UTC timestamp of when the category was created
 * @param updatedAt   UTC timestamp of the last update
 */
public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        boolean active,
        Long parentCategoryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public CategoryResponse(Long id, String name, String slug, String description, String imageUrl,
                           boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, slug, description, imageUrl, active, null, createdAt, updatedAt);
    }
}
