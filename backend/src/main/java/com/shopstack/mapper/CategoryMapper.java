package com.shopstack.mapper;

import com.shopstack.dto.category.CategoryResponse;
import com.shopstack.entity.Category;
import org.springframework.stereotype.Component;

/**
 * CategoryMapper — Component mapping {@link Category} entities to {@link CategoryResponse} DTOs.
 *
 * <p>Follows the same pattern as {@link VendorProfileMapper} — a lightweight {@code @Component}
 * with a single {@code toResponse} method. No entity is ever exposed directly to the client.
 */
@Component
public class CategoryMapper {

    /**
     * Converts a {@link Category} entity to a {@link CategoryResponse} DTO.
     *
     * @param category the persistent category entity
     * @return {@link CategoryResponse} containing client-safe fields, or {@code null} if input is null
     */
    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.isActive(),
                category.getParentCategory() != null ? category.getParentCategory().getId() : null,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
