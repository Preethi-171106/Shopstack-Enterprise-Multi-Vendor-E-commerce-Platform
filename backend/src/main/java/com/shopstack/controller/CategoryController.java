package com.shopstack.controller;

import com.shopstack.dto.category.CategoryCreateRequest;
import com.shopstack.dto.category.CategoryResponse;
import com.shopstack.dto.category.CategoryStatusRequest;
import com.shopstack.dto.category.CategoryUpdateRequest;
import com.shopstack.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CategoryController — REST Controller handling product category endpoints.
 *
 * <p>Admin routes (base: {@code /api/admin/categories}) require {@code ROLE_ADMIN}.
 * <p>Public routes (base: {@code /api/categories}) are accessible without authentication.
 *
 * <p>Authorization is enforced at the SecurityConfig level; this controller does not
 * use {@code @PreAuthorize} annotations to avoid circular dependencies and keep a
 * centralized security configuration.
 */
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // =========================================================================
    // Admin Endpoints — ROLE_ADMIN required (enforced in SecurityConfig)
    // =========================================================================

    /**
     * Returns all categories (including inactive) for admin management.
     *
     * <p><b>Endpoint:</b> {@code GET /api/admin/categories}
     * <p><b>Access:</b> ADMIN only
     *
     * @return HTTP 200 OK with a list of all {@link CategoryResponse} DTOs
     */
    @GetMapping("/api/admin/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategoriesForAdmin() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Creates a new product category.
     *
     * <p><b>Endpoint:</b> {@code POST /api/admin/categories}
     * <p><b>Access:</b> ADMIN only
     * <p>The slug is auto-generated from the name. The active flag is set to {@code true}.
     *
     * @param request the creation request with name, description, and imageUrl
     * @return HTTP 201 Created with the created {@link CategoryResponse}
     */
    @PostMapping("/api/admin/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing category's name, description, and imageUrl.
     *
     * <p><b>Endpoint:</b> {@code PUT /api/admin/categories/{id}}
     * <p><b>Access:</b> ADMIN only
     * <p>If the name changes, the slug is regenerated. Clients cannot modify id, slug,
     * active, createdAt, or updatedAt.
     *
     * @param id      the category ID to update
     * @param request the update request with name, description, and imageUrl
     * @return HTTP 200 OK with the updated {@link CategoryResponse}
     */
    @PutMapping("/api/admin/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Activates or deactivates a category (soft status change).
     *
     * <p><b>Endpoint:</b> {@code PATCH /api/admin/categories/{id}/status}
     * <p><b>Access:</b> ADMIN only
     * <p>Accepts {@code {"active": true}} or {@code {"active": false}}.
     * Inactive categories are hidden from public endpoints.
     *
     * @param id      the category ID
     * @param request the status request with the new active boolean
     * @return HTTP 200 OK with the updated {@link CategoryResponse}
     */
    @PatchMapping("/api/admin/categories/{id}/status")
    public ResponseEntity<CategoryResponse> updateCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody CategoryStatusRequest request
    ) {
        CategoryResponse response = categoryService.updateCategoryStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Permanently deletes a category by ID.
     *
     * <p><b>Endpoint:</b> {@code DELETE /api/admin/categories/{id}}
     * <p><b>Access:</b> ADMIN only
     *
     * @param id the category ID to delete
     * @return HTTP 204 No Content if successful, or HTTP 404 if not found
     */
    @DeleteMapping("/api/admin/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Public Endpoints — No authentication required
    // =========================================================================

    /**
     * Returns a list of all active product categories.
     *
     * <p><b>Endpoint:</b> {@code GET /api/categories}
     * <p><b>Access:</b> Public (no JWT required)
     *
     * @return HTTP 200 OK with a list of active {@link CategoryResponse} DTOs
     */
    @GetMapping("/api/categories")
    public ResponseEntity<List<CategoryResponse>> getActiveCategories() {
        List<CategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Returns a single active category by its ID.
     *
     * <p><b>Endpoint:</b> {@code GET /api/categories/{id}}
     * <p><b>Access:</b> Public (no JWT required)
     * <p>Returns HTTP 404 if the category does not exist or is inactive.
     *
     * @param id the category ID
     * @return HTTP 200 OK with the {@link CategoryResponse}, or HTTP 404
     */
    @GetMapping("/api/categories/{id}")
    public ResponseEntity<CategoryResponse> getActiveCategoryById(@PathVariable Long id) {
        CategoryResponse response = categoryService.getActiveCategoryById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/categories/{id}/subcategories")
    public ResponseEntity<List<CategoryResponse>> getSubcategories(@PathVariable Long id) {
        List<CategoryResponse> response = categoryService.getSubcategories(id);
        return ResponseEntity.ok(response);
    }
}
