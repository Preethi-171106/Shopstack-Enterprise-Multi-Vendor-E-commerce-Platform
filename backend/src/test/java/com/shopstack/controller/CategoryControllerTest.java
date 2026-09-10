package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.category.CategoryCreateRequest;
import com.shopstack.dto.category.CategoryResponse;
import com.shopstack.dto.category.CategoryStatusRequest;
import com.shopstack.dto.category.CategoryUpdateRequest;
import com.shopstack.exception.CategoryAlreadyExistsException;
import com.shopstack.exception.CategoryNotFoundException;
import com.shopstack.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CategoryControllerTest — MockMvc integration tests for Category Management APIs.
 *
 * <p>Covers:
 * <ul>
 *   <li>ADMIN CRUD (create, update, status patch, delete) — success cases</li>
 *   <li>CUSTOMER and VENDOR — forbidden (HTTP 403) on admin endpoints</li>
 *   <li>Unauthenticated — unauthorized (HTTP 401) on admin endpoints</li>
 *   <li>Public listing and retrieval of active categories</li>
 *   <li>Inactive categories not publicly visible (HTTP 404)</li>
 *   <li>Duplicate category name — HTTP 409 Conflict</li>
 *   <li>Invalid input — HTTP 400 Bad Request</li>
 *   <li>Missing category — HTTP 404 Not Found</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    // =========================================================================
    // Test Fixtures
    // =========================================================================

    private CategoryResponse sampleResponse(Long id, String name, String slug, boolean active) {
        return new CategoryResponse(
                id, name, slug, "Test description", null, active,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    // =========================================================================
    // Admin — Create Category
    // =========================================================================

    @Nested
    @DisplayName("POST /api/admin/categories — Create Category")
    class CreateCategory {

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — creates category successfully (201 Created)")
        void adminCreateCategory_Success() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(
                    "Electronics", "All electronics", null
            );
            CategoryResponse response = sampleResponse(1L, "Electronics", "electronics", true);

            given(categoryService.createCategory(any(CategoryCreateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Electronics"))
                    .andExpect(jsonPath("$.slug").value("electronics"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — returns 403 Forbidden on admin create")
        void customerCreateCategory_Returns403() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("Electronics", null, null);

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 403 Forbidden on admin create")
        void vendorCreateCategory_Returns403() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("Electronics", null, null);

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 Unauthorized on admin create")
        void unauthenticatedCreateCategory_Returns401() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("Electronics", null, null);

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — duplicate name returns 409 Conflict")
        void duplicateCategoryName_Returns409() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("electronics", null, null);

            given(categoryService.createCategory(any(CategoryCreateRequest.class)))
                    .willThrow(new CategoryAlreadyExistsException("Category with name 'electronics' already exists"));

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Category with name 'electronics' already exists"));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — blank name returns 400 Bad Request")
        void blankName_Returns400() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("", null, null);

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — invalid imageUrl returns 400 Bad Request")
        void invalidImageUrl_Returns400() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(
                    "Electronics", null, "not-a-url"
            );

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // =========================================================================
    // Admin — Update Category
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/admin/categories/{id} — Update Category")
    class UpdateCategory {

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — updates category successfully (200 OK)")
        void adminUpdateCategory_Success() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest(
                    "Home & Kitchen", "Kitchen items", null
            );
            CategoryResponse response = sampleResponse(1L, "Home & Kitchen", "home-kitchen", true);

            given(categoryService.updateCategory(eq(1L), any(CategoryUpdateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(put("/api/admin/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Home & Kitchen"))
                    .andExpect(jsonPath("$.slug").value("home-kitchen"));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — returns 403 Forbidden on admin update")
        void customerUpdateCategory_Returns403() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest("Electronics", null, null);

            mockMvc.perform(put("/api/admin/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 403 Forbidden on admin update")
        void vendorUpdateCategory_Returns403() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest("Electronics", null, null);

            mockMvc.perform(put("/api/admin/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 Unauthorized on admin update")
        void unauthenticatedUpdateCategory_Returns401() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest("Electronics", null, null);

            mockMvc.perform(put("/api/admin/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — update with missing category returns 404 Not Found")
        void updateMissingCategory_Returns404() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest("Electronics", null, null);

            given(categoryService.updateCategory(eq(999L), any(CategoryUpdateRequest.class)))
                    .willThrow(new CategoryNotFoundException("Category with ID 999 not found"));

            mockMvc.perform(put("/api/admin/categories/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — duplicate name on update returns 409 Conflict")
        void duplicateNameOnUpdate_Returns409() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest("Electronics", null, null);

            given(categoryService.updateCategory(eq(2L), any(CategoryUpdateRequest.class)))
                    .willThrow(new CategoryAlreadyExistsException("Category with name 'Electronics' already exists"));

            mockMvc.perform(put("/api/admin/categories/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    // =========================================================================
    // Admin — Status Toggle
    // =========================================================================

    @Nested
    @DisplayName("PATCH /api/admin/categories/{id}/status — Toggle Status")
    class UpdateCategoryStatus {

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — deactivates category successfully (200 OK)")
        void adminDeactivateCategory_Success() throws Exception {
            CategoryStatusRequest request = new CategoryStatusRequest(false);
            CategoryResponse response = sampleResponse(1L, "Electronics", "electronics", false);

            given(categoryService.updateCategoryStatus(eq(1L), any(CategoryStatusRequest.class)))
                    .willReturn(response);

            mockMvc.perform(patch("/api/admin/categories/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — activates category successfully (200 OK)")
        void adminActivateCategory_Success() throws Exception {
            CategoryStatusRequest request = new CategoryStatusRequest(true);
            CategoryResponse response = sampleResponse(1L, "Electronics", "electronics", true);

            given(categoryService.updateCategoryStatus(eq(1L), any(CategoryStatusRequest.class)))
                    .willReturn(response);

            mockMvc.perform(patch("/api/admin/categories/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — returns 403 Forbidden on status toggle")
        void customerStatusToggle_Returns403() throws Exception {
            CategoryStatusRequest request = new CategoryStatusRequest(false);

            mockMvc.perform(patch("/api/admin/categories/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 403 Forbidden on status toggle")
        void vendorStatusToggle_Returns403() throws Exception {
            CategoryStatusRequest request = new CategoryStatusRequest(false);

            mockMvc.perform(patch("/api/admin/categories/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 Unauthorized on status toggle")
        void unauthenticatedStatusToggle_Returns401() throws Exception {
            CategoryStatusRequest request = new CategoryStatusRequest(false);

            mockMvc.perform(patch("/api/admin/categories/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — missing category on status toggle returns 404")
        void statusToggleMissingCategory_Returns404() throws Exception {
            CategoryStatusRequest request = new CategoryStatusRequest(false);

            given(categoryService.updateCategoryStatus(eq(999L), any(CategoryStatusRequest.class)))
                    .willThrow(new CategoryNotFoundException("Category with ID 999 not found"));

            mockMvc.perform(patch("/api/admin/categories/999/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — missing active field returns 400 Bad Request")
        void missingActiveField_Returns400() throws Exception {
            // Send empty body / null active field
            mockMvc.perform(patch("/api/admin/categories/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"active\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // =========================================================================
    // Admin — Delete Category
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/admin/categories/{id} — Delete Category")
    class DeleteCategory {

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — deletes category successfully (204 No Content)")
        void adminDeleteCategory_Success() throws Exception {
            willDoNothing().given(categoryService).deleteCategory(1L);

            mockMvc.perform(delete("/api/admin/categories/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — returns 403 Forbidden on admin delete")
        void customerDeleteCategory_Returns403() throws Exception {
            mockMvc.perform(delete("/api/admin/categories/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 403 Forbidden on admin delete")
        void vendorDeleteCategory_Returns403() throws Exception {
            mockMvc.perform(delete("/api/admin/categories/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 Unauthorized on admin delete")
        void unauthenticatedDeleteCategory_Returns401() throws Exception {
            mockMvc.perform(delete("/api/admin/categories/1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — delete missing category returns 404 Not Found")
        void deleteMissingCategory_Returns404() throws Exception {
            willThrow(new CategoryNotFoundException("Category with ID 999 not found"))
                    .given(categoryService).deleteCategory(999L);

            mockMvc.perform(delete("/api/admin/categories/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // =========================================================================
    // Public — List Active Categories
    // =========================================================================

    @Nested
    @DisplayName("GET /api/categories — Public List Active Categories")
    class ListActiveCategories {

        @Test
        @DisplayName("Public — returns list of active categories (200 OK)")
        void publicListActiveCategories_Success() throws Exception {
            List<CategoryResponse> categories = List.of(
                    sampleResponse(1L, "Electronics", "electronics", true),
                    sampleResponse(2L, "Home & Kitchen", "home-kitchen", true)
            );

            given(categoryService.getActiveCategories()).willReturn(categories);

            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Electronics"))
                    .andExpect(jsonPath("$[1].name").value("Home & Kitchen"));
        }

        @Test
        @DisplayName("Public — returns empty list when no active categories (200 OK)")
        void publicListActiveCategories_Empty() throws Exception {
            given(categoryService.getActiveCategories()).willReturn(List.of());

            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // =========================================================================
    // Public — Get Single Active Category by ID
    // =========================================================================

    @Nested
    @DisplayName("GET /api/categories/{id} — Public Get Active Category by ID")
    class GetActiveCategoryById {

        @Test
        @DisplayName("Public — returns active category by ID (200 OK)")
        void publicGetActiveCategory_Success() throws Exception {
            CategoryResponse response = sampleResponse(1L, "Electronics", "electronics", true);

            given(categoryService.getActiveCategoryById(1L)).willReturn(response);

            mockMvc.perform(get("/api/categories/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Electronics"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("Public — inactive category returns 404 Not Found")
        void publicGetInactiveCategory_Returns404() throws Exception {
            given(categoryService.getActiveCategoryById(2L))
                    .willThrow(new CategoryNotFoundException("Category with ID 2 not found or is not active"));

            mockMvc.perform(get("/api/categories/2"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Public — non-existent category returns 404 Not Found")
        void publicGetNonExistentCategory_Returns404() throws Exception {
            given(categoryService.getActiveCategoryById(999L))
                    .willThrow(new CategoryNotFoundException("Category with ID 999 not found or is not active"));

            mockMvc.perform(get("/api/categories/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
