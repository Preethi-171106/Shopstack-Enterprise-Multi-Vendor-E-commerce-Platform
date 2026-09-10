package com.shopstack.service;

import com.shopstack.dto.category.CategoryCreateRequest;
import com.shopstack.dto.category.CategoryResponse;
import com.shopstack.dto.category.CategoryStatusRequest;
import com.shopstack.dto.category.CategoryUpdateRequest;
import com.shopstack.entity.Category;
import com.shopstack.exception.CategoryAlreadyExistsException;
import com.shopstack.exception.CategoryNotFoundException;
import com.shopstack.mapper.CategoryMapper;
import com.shopstack.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * CategoryService — Business service managing product category lifecycle.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Create a new category with auto-generated slug</li>
 *   <li>Update name, description, and imageUrl (regenerates slug if name changes)</li>
 *   <li>Soft-toggle active status (activate / deactivate)</li>
 *   <li>Delete a category by ID</li>
 *   <li>Return only active categories for public endpoints</li>
 * </ul>
 *
 * <p>Slug rules:
 * <ul>
 *   <li>Lowercase the name</li>
 *   <li>Replace any sequence of non-alphanumeric characters with a single hyphen</li>
 *   <li>Strip leading and trailing hyphens</li>
 *   <li>Example: "Home &amp; Kitchen" → "home-kitchen"</li>
 * </ul>
 *
 * <p>Uniqueness is enforced case-insensitively on name before hitting the DB constraint.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final com.shopstack.repository.ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper,
                          com.shopstack.repository.ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.productRepository = productRepository;
    }

    // =========================================================================
    // Admin Operations
    // =========================================================================

    /**
     * Returns all categories (both active and inactive) for admin management.
     *
     * @return list of all categories
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    /**
     * Creates a new active category with an auto-generated slug.
     *
     * <p>The name must be unique (case-insensitive). The slug is derived from the name
     * and must also be unique. If the generated slug conflicts with an existing one,
     * a {@link CategoryAlreadyExistsException} is thrown.
     *
     * @param request the creation request containing name, description, and imageUrl
     * @return the created {@link CategoryResponse}
     * @throws CategoryAlreadyExistsException if a category with the same name or slug already exists
     */
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        String trimmedName = request.name().trim();
        String slug = generateSlug(trimmedName);

        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new CategoryAlreadyExistsException(
                    "Category with name '" + trimmedName + "' already exists"
            );
        }

        if (categoryRepository.existsBySlug(slug)) {
            throw new CategoryAlreadyExistsException(
                    "A category with equivalent slug '" + slug + "' already exists"
            );
        }

        Category parent = resolveParentCategory(request.parentCategoryId(), null);

        Category category = Category.builder()
                .name(trimmedName)
                .slug(slug)
                .description(request.description() != null ? request.description().trim() : null)
                .imageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null)
                .parentCategory(parent)
                .active(true)
                .build();

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    /**
     * Updates an existing category's name, description, and imageUrl.
     *
     * <p>If the name changes, the slug is regenerated. The new name must not conflict
     * with any other existing category (case-insensitive, excluding the current category).
     *
     * @param id      the category ID to update
     * @param request the update request containing new name, description, and imageUrl
     * @return the updated {@link CategoryResponse}
     * @throws CategoryNotFoundException      if no category with the given ID exists
     * @throws CategoryAlreadyExistsException if another category has the same name or slug
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with ID " + id + " not found"
                ));

        String trimmedName = request.name().trim();
        String newSlug = generateSlug(trimmedName);

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new CategoryAlreadyExistsException(
                    "Category with name '" + trimmedName + "' already exists"
            );
        }

        if (categoryRepository.existsBySlugAndIdNot(newSlug, id)) {
            throw new CategoryAlreadyExistsException(
                    "A category with equivalent slug '" + newSlug + "' already exists"
            );
        }

        Category parent = resolveParentCategory(request.parentCategoryId(), id);

        category.setName(trimmedName);
        category.setSlug(newSlug);
        category.setDescription(request.description() != null ? request.description().trim() : null);
        category.setImageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null);
        category.setParentCategory(parent);

        Category updated = categoryRepository.save(category);
        return categoryMapper.toResponse(updated);
    }

    /**
     * Activates or deactivates an existing category (soft status change).
     *
     * <p>The category record is preserved in the database. When deactivated, it is hidden
     * from public listing and detail endpoints. Admins always have access regardless.
     *
     * @param id      the category ID
     * @param request the status request with {@code active} boolean
     * @return the updated {@link CategoryResponse}
     * @throws CategoryNotFoundException if no category with the given ID exists
     */
    @Transactional
    public CategoryResponse updateCategoryStatus(Long id, CategoryStatusRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with ID " + id + " not found"
                ));

        category.setActive(request.active());

        Category updated = categoryRepository.save(category);
        return categoryMapper.toResponse(updated);
    }

    /**
     * Permanently deletes a category by ID.
     *
     * @param id the category ID to delete
     * @throws CategoryNotFoundException if no category with the given ID exists
     */
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException(
                    "Category with ID " + id + " not found"
            );
        }
        if (categoryRepository.existsByParentCategoryId(id) || productRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Cannot delete category with subcategories or products attached");
        }
        categoryRepository.deleteById(id);
    }

    // =========================================================================
    // Public Operations
    // =========================================================================

    /**
     * Returns all currently active categories. Inactive categories are excluded.
     *
     * @return list of active {@link CategoryResponse} DTOs
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrue()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    /**
     * Returns a single active category by its ID.
     *
     * <p>Returns 404 if the category does not exist or is inactive.
     *
     * @param id the category ID
     * @return the {@link CategoryResponse} for the active category
     * @throws CategoryNotFoundException if the category is missing or inactive
     */
    @Transactional(readOnly = true)
    public CategoryResponse getActiveCategoryById(Long id) {
        Category category = categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with ID " + id + " not found or is not active"
                ));
        return categoryMapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubcategories(Long parentCategoryId) {
        Category parent = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with ID " + parentCategoryId + " not found"
                ));
        return categoryRepository.findByParentCategoryId(parent.getId())
                .stream()
                .filter(Category::isActive)
                .map(categoryMapper::toResponse)
                .toList();
    }

    // =========================================================================
    // Internal Helpers
    // =========================================================================

    /**
     * Generates a URL-safe slug from a category name.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Lowercase the name</li>
     *   <li>Replace each sequence of non-alphanumeric characters with a single hyphen</li>
     *   <li>Strip leading and trailing hyphens</li>
     * </ol>
     *
     * <p>Examples:
     * <ul>
     *   <li>"Home &amp; Kitchen" → "home-kitchen"</li>
     *   <li>"Electronics" → "electronics"</li>
     *   <li>"Men's Clothing" → "men-s-clothing"</li>
     *   <li>"  Books  " → "books"</li>
     * </ul>
     *
     * @param name the human-readable category name
     * @return the generated URL-safe slug
     */
    String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private Category resolveParentCategory(Long parentCategoryId, Long currentCategoryId) {
        if (parentCategoryId == null) {
            return null;
        }
        Category parent = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Parent category with ID " + parentCategoryId + " not found"
                ));
        if (currentCategoryId != null && parent.getId().equals(currentCategoryId)) {
            throw new IllegalArgumentException("Category cannot be its own parent");
        }
        validateNoCircularParent(parent, currentCategoryId);
        return parent;
    }

    private void validateNoCircularParent(Category candidateParent, Long currentCategoryId) {
        if (candidateParent == null) return;
        Set<Long> seen = new java.util.HashSet<>();
        Category cursor = candidateParent;
        while (cursor != null && seen.add(cursor.getId())) {
            if (cursor.getId().equals(currentCategoryId)) {
                throw new IllegalArgumentException("Circular category relationship is not allowed");
            }
            cursor = cursor.getParentCategory();
        }
    }
}
