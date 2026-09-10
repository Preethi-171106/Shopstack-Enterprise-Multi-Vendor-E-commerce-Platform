package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.product.ProductCreateRequest;
import com.shopstack.dto.product.ProductResponse;
import com.shopstack.dto.product.ProductStatusRequest;
import com.shopstack.dto.product.ProductUpdateRequest;
import com.shopstack.exception.ProductAlreadyExistsException;
import com.shopstack.exception.ProductNotFoundException;
import com.shopstack.exception.ProductOwnershipException;
import com.shopstack.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
 * ProductControllerTest — MockMvc integration tests for all Product Management APIs.
 *
 * <p>Covers:
 * <ul>
 *   <li>VENDOR CRUD (create, list, get, update, delete, status) — success cases</li>
 *   <li>VENDOR cannot access another vendor's product — 403 ownership check</li>
 *   <li>CUSTOMER and unauthenticated — forbidden/unauthorized on vendor/admin endpoints</li>
 *   <li>ADMIN can view and manage all products</li>
 *   <li>Public product listing with search and category filter</li>
 *   <li>Inactive products not publicly visible (HTTP 404)</li>
 *   <li>Duplicate SKU returns 409 Conflict</li>
 *   <li>Invalid category returns 404 Not Found</li>
 *   <li>Bean validation returns 400 Bad Request</li>
 * </ul>
 *
 * <p>Uses {@code @MockBean ProductService} to avoid any real DB access.
 * Spring context uses H2 (configured in src/test/resources/application.properties).
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    // =========================================================================
    // Test Fixtures
    // =========================================================================

    private ProductResponse sampleProduct(Long id, String name, String sku, boolean active) {
        return new ProductResponse(
                id,
                1L,
                "TechStore",
                2L,
                "Electronics",
                "electronics",
                name,
                name.toLowerCase().replace(" ", "-"),
                "A great product",
                new BigDecimal("999.99"),
                new BigDecimal("1199.99"),
                50,
                sku,
                null,
                active,
                false,
                BigDecimal.ZERO,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private ProductCreateRequest validCreateRequest() {
        return new ProductCreateRequest(
                "iPhone 15 Pro",
                2L,
                new BigDecimal("999.99"),
                new BigDecimal("1199.99"),
                "APPLE-IP15-PRO",
                10,
                "Latest iPhone",
                null
        );
    }

    private ProductUpdateRequest validUpdateRequest() {
        return new ProductUpdateRequest(
                "iPhone 15 Pro Max",
                2L,
                new BigDecimal("1099.99"),
                new BigDecimal("1299.99"),
                "APPLE-IP15-PROMAX",
                5,
                "Updated iPhone",
                null
        );
    }

    // =========================================================================
    // Vendor — Create Product
    // =========================================================================

    @Nested
    @DisplayName("POST /api/vendors/products — Vendor Create Product")
    class VendorCreateProduct {

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — creates product successfully (201 Created)")
        void vendorCreateProduct_Success() throws Exception {
            ProductResponse response = sampleProduct(1L, "iPhone 15 Pro", "APPLE-IP15-PRO", true);
            given(productService.createProduct(any(ProductCreateRequest.class))).willReturn(response);

            mockMvc.perform(post("/api/vendors/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("iPhone 15 Pro"))
                    .andExpect(jsonPath("$.sku").value("APPLE-IP15-PRO"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — returns 403 Forbidden on vendor create")
        void customerCreateProduct_Returns403() throws Exception {
            mockMvc.perform(post("/api/vendors/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 on vendor create")
        void unauthenticatedCreateProduct_Returns401() throws Exception {
            mockMvc.perform(post("/api/vendors/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — duplicate SKU returns 409 Conflict")
        void duplicateSku_Returns409() throws Exception {
            given(productService.createProduct(any(ProductCreateRequest.class)))
                    .willThrow(new ProductAlreadyExistsException("A product with SKU 'APPLE-IP15-PRO' already exists"));

            mockMvc.perform(post("/api/vendors/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("A product with SKU 'APPLE-IP15-PRO' already exists"));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — blank product name returns 400 Bad Request")
        void blankName_Returns400() throws Exception {
            ProductCreateRequest bad = new ProductCreateRequest(
                    "", 2L, new BigDecimal("10.00"), null, "SKU-1", 0, null, null
            );
            mockMvc.perform(post("/api/vendors/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — null category ID returns 400 Bad Request")
        void nullCategoryId_Returns400() throws Exception {
            ProductCreateRequest bad = new ProductCreateRequest(
                    "Valid Name", null, new BigDecimal("10.00"), null, "SKU-2", 0, null, null
            );
            mockMvc.perform(post("/api/vendors/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — null price returns 400 Bad Request")
        void nullPrice_Returns400() throws Exception {
            ProductCreateRequest bad = new ProductCreateRequest(
                    "Valid Name", 2L, null, null, "SKU-3", 0, null, null
            );
            mockMvc.perform(post("/api/vendors/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — negative price returns 400 Bad Request")
        void negativePriceReturns400() throws Exception {
            ProductCreateRequest bad = new ProductCreateRequest(
                    "Valid Name", 2L, new BigDecimal("-1.00"), null, "SKU-4", 0, null, null
            );
            mockMvc.perform(post("/api/vendors/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — invalid image URL returns 400 Bad Request")
        void invalidImageUrl_Returns400() throws Exception {
            ProductCreateRequest bad = new ProductCreateRequest(
                    "Valid Name", 2L, new BigDecimal("10.00"), null, "SKU-5", 0, null, "not-a-url"
            );
            mockMvc.perform(post("/api/vendors/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — invalid category ID returns 404 Not Found")
        void invalidCategory_Returns404() throws Exception {
            given(productService.createProduct(any(ProductCreateRequest.class)))
                    .willThrow(new com.shopstack.exception.CategoryNotFoundException("Category with ID 999 not found"));

            ProductCreateRequest req = new ProductCreateRequest(
                    "Valid Name", 999L, new BigDecimal("10.00"), null, "SKU-6", 0, null, null
            );
            mockMvc.perform(post("/api/vendors/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // =========================================================================
    // Vendor — List Own Products
    // =========================================================================

    @Nested
    @DisplayName("GET /api/vendors/products — Vendor List Own Products")
    class VendorListProducts {

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — retrieves own products (200 OK)")
        void vendorListProducts_Success() throws Exception {
            List<ProductResponse> products = List.of(
                    sampleProduct(1L, "iPhone 15", "SKU-A", true),
                    sampleProduct(2L, "MacBook Pro", "SKU-B", false)
            );
            given(productService.getVendorProducts()).willReturn(products);

            mockMvc.perform(get("/api/vendors/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("iPhone 15"))
                    .andExpect(jsonPath("$[1].name").value("MacBook Pro"));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — returns 403 Forbidden on vendor list")
        void customerListProducts_Returns403() throws Exception {
            mockMvc.perform(get("/api/vendors/products"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 on vendor list")
        void unauthenticatedListProducts_Returns401() throws Exception {
            mockMvc.perform(get("/api/vendors/products"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }

    // =========================================================================
    // Vendor — Get Own Product By ID
    // =========================================================================

    @Nested
    @DisplayName("GET /api/vendors/products/{id} — Vendor Get Product By ID")
    class VendorGetProductById {

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — retrieves own product by ID (200 OK)")
        void vendorGetProduct_Success() throws Exception {
            ProductResponse response = sampleProduct(1L, "iPhone 15", "APPLE-IP15", true);
            given(productService.getVendorProductById(1L)).willReturn(response);

            mockMvc.perform(get("/api/vendors/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("iPhone 15"));
        }

        @Test
        @WithMockUser(username = "vendor2@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 403 when accessing another vendor's product")
        void vendorAccessOtherVendorProduct_Returns403() throws Exception {
            given(productService.getVendorProductById(1L))
                    .willThrow(new ProductOwnershipException("Access denied: you do not own product with ID 1"));

            mockMvc.perform(get("/api/vendors/products/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("Access denied: you do not own product with ID 1"));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 404 for missing product")
        void vendorGetMissingProduct_Returns404() throws Exception {
            given(productService.getVendorProductById(999L))
                    .willThrow(new ProductNotFoundException("Product with ID 999 not found"));

            mockMvc.perform(get("/api/vendors/products/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // =========================================================================
    // Vendor — Update Product
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/vendors/products/{id} — Vendor Update Product")
    class VendorUpdateProduct {

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — updates own product successfully (200 OK)")
        void vendorUpdateProduct_Success() throws Exception {
            ProductResponse response = sampleProduct(1L, "iPhone 15 Pro Max", "APPLE-IP15-PROMAX", true);
            given(productService.updateVendorProduct(eq(1L), any(ProductUpdateRequest.class))).willReturn(response);

            mockMvc.perform(put("/api/vendors/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("iPhone 15 Pro Max"));
        }

        @Test
        @WithMockUser(username = "vendor2@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 403 when updating another vendor's product")
        void vendorUpdateOtherVendorProduct_Returns403() throws Exception {
            given(productService.updateVendorProduct(eq(1L), any(ProductUpdateRequest.class)))
                    .willThrow(new ProductOwnershipException("Access denied: you do not own product with ID 1"));

            mockMvc.perform(put("/api/vendors/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 404 when updating missing product")
        void vendorUpdateMissingProduct_Returns404() throws Exception {
            given(productService.updateVendorProduct(eq(999L), any(ProductUpdateRequest.class)))
                    .willThrow(new ProductNotFoundException("Product with ID 999 not found"));

            mockMvc.perform(put("/api/vendors/products/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — duplicate SKU on update returns 409 Conflict")
        void vendorUpdateDuplicateSku_Returns409() throws Exception {
            given(productService.updateVendorProduct(eq(1L), any(ProductUpdateRequest.class)))
                    .willThrow(new ProductAlreadyExistsException("A product with SKU 'APPLE-IP15-PROMAX' already exists"));

            mockMvc.perform(put("/api/vendors/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    // =========================================================================
    // Vendor — Delete Product
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/vendors/products/{id} — Vendor Delete Product")
    class VendorDeleteProduct {

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — soft-deletes own product (204 No Content)")
        void vendorDeleteProduct_Success() throws Exception {
            willDoNothing().given(productService).deleteVendorProduct(1L);

            mockMvc.perform(delete("/api/vendors/products/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "vendor2@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 403 when deleting another vendor's product")
        void vendorDeleteOtherVendorProduct_Returns403() throws Exception {
            willThrow(new ProductOwnershipException("Access denied: you do not own product with ID 1"))
                    .given(productService).deleteVendorProduct(1L);

            mockMvc.perform(delete("/api/vendors/products/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 on vendor delete")
        void unauthenticatedDeleteProduct_Returns401() throws Exception {
            mockMvc.perform(delete("/api/vendors/products/1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 404 for missing product")
        void vendorDeleteMissingProduct_Returns404() throws Exception {
            willThrow(new ProductNotFoundException("Product with ID 999 not found"))
                    .given(productService).deleteVendorProduct(999L);

            mockMvc.perform(delete("/api/vendors/products/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // =========================================================================
    // Vendor — Status Toggle
    // =========================================================================

    @Nested
    @DisplayName("PATCH /api/vendors/products/{id}/status — Vendor Toggle Status")
    class VendorProductStatus {

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — deactivates own product (200 OK)")
        void vendorDeactivateProduct_Success() throws Exception {
            ProductStatusRequest request = new ProductStatusRequest(false);
            ProductResponse response = sampleProduct(1L, "iPhone 15", "APPLE-IP15", false);
            given(productService.updateVendorProductStatus(eq(1L), any(ProductStatusRequest.class))).willReturn(response);

            mockMvc.perform(patch("/api/vendors/products/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — null active field returns 400 Bad Request")
        void vendorNullActiveField_Returns400() throws Exception {
            mockMvc.perform(patch("/api/vendors/products/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"active\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @WithMockUser(username = "vendor2@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 403 when toggling another vendor's product status")
        void vendorToggleOtherVendorStatus_Returns403() throws Exception {
            given(productService.updateVendorProductStatus(eq(1L), any(ProductStatusRequest.class)))
                    .willThrow(new ProductOwnershipException("Access denied: you do not own product with ID 1"));

            mockMvc.perform(patch("/api/vendors/products/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"active\": false}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    // =========================================================================
    // Admin — Product Management
    // =========================================================================

    @Nested
    @DisplayName("Admin Product Management — /api/admin/products/**")
    class AdminProductManagement {

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — lists all products (200 OK)")
        void adminListAllProducts_Success() throws Exception {
            List<ProductResponse> products = List.of(
                    sampleProduct(1L, "iPhone 15", "SKU-A", true),
                    sampleProduct(2L, "Samsung TV", "SKU-B", false)
            );
            given(productService.getAllProductsForAdmin()).willReturn(products);

            mockMvc.perform(get("/api/admin/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER — returns 403 on admin product list")
        void customerAccessAdminProducts_Returns403() throws Exception {
            mockMvc.perform(get("/api/admin/products"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR — returns 403 on admin product list")
        void vendorAccessAdminProducts_Returns403() throws Exception {
            mockMvc.perform(get("/api/admin/products"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Unauthenticated — returns 401 on admin product list")
        void unauthenticatedAccessAdminProducts_Returns401() throws Exception {
            mockMvc.perform(get("/api/admin/products"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — gets product by ID (200 OK)")
        void adminGetProductById_Success() throws Exception {
            ProductResponse response = sampleProduct(1L, "iPhone 15", "SKU-A", true);
            given(productService.getAdminProductById(1L)).willReturn(response);

            mockMvc.perform(get("/api/admin/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — returns 404 for missing product")
        void adminGetMissingProduct_Returns404() throws Exception {
            given(productService.getAdminProductById(999L))
                    .willThrow(new ProductNotFoundException("Product with ID 999 not found"));

            mockMvc.perform(get("/api/admin/products/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — updates any product (200 OK)")
        void adminUpdateProduct_Success() throws Exception {
            ProductResponse response = sampleProduct(1L, "iPhone 15 Pro Max", "APPLE-IP15-PROMAX", true);
            given(productService.updateAdminProduct(eq(1L), any(ProductUpdateRequest.class))).willReturn(response);

            mockMvc.perform(put("/api/admin/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("iPhone 15 Pro Max"));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — deactivates any product (200 OK)")
        void adminDeactivateProduct_Success() throws Exception {
            ProductStatusRequest request = new ProductStatusRequest(false);
            ProductResponse response = sampleProduct(1L, "iPhone 15", "SKU-A", false);
            given(productService.updateAdminProductStatus(eq(1L), any(ProductStatusRequest.class))).willReturn(response);

            mockMvc.perform(patch("/api/admin/products/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN — soft-deletes any product (204 No Content)")
        void adminDeleteProduct_Success() throws Exception {
            willDoNothing().given(productService).deleteAdminProduct(1L);

            mockMvc.perform(delete("/api/admin/products/1"))
                    .andExpect(status().isNoContent());
        }
    }

    // =========================================================================
    // Public — Product Listing and Detail
    // =========================================================================

    @Nested
    @DisplayName("Public Product APIs — /api/products/**")
    class PublicProducts {

        @Test
        @DisplayName("Public — lists active products with pagination (200 OK)")
        void publicListProducts_Success() throws Exception {
            List<ProductResponse> content = List.of(
                    sampleProduct(1L, "iPhone 15", "SKU-A", true),
                    sampleProduct(2L, "MacBook Air", "SKU-B", true)
            );
            PageImpl<ProductResponse> page = new PageImpl<>(content, PageRequest.of(0, 20), 2);
            given(productService.getPublicProducts(isNull(), isNull(), eq(0), eq(20))).willReturn(page);

            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].name").value("iPhone 15"))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Public — search products by name (200 OK)")
        void publicSearchProducts_Success() throws Exception {
            List<ProductResponse> content = List.of(sampleProduct(1L, "iPhone 15", "SKU-A", true));
            PageImpl<ProductResponse> page = new PageImpl<>(content, PageRequest.of(0, 20), 1);
            given(productService.getPublicProducts(eq("iphone"), isNull(), eq(0), eq(20))).willReturn(page);

            mockMvc.perform(get("/api/products").param("search", "iphone"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("Public — filter products by category (200 OK)")
        void publicFilterByCategory_Success() throws Exception {
            List<ProductResponse> content = List.of(sampleProduct(1L, "iPhone 15", "SKU-A", true));
            PageImpl<ProductResponse> page = new PageImpl<>(content, PageRequest.of(0, 20), 1);
            given(productService.getPublicProducts(isNull(), eq(2L), eq(0), eq(20))).willReturn(page);

            mockMvc.perform(get("/api/products").param("categoryId", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].categoryId").value(2L));
        }

        @Test
        @DisplayName("Public — gets active product by ID (200 OK)")
        void publicGetProductById_Success() throws Exception {
            ProductResponse response = sampleProduct(1L, "iPhone 15", "SKU-A", true);
            given(productService.getPublicProductById(1L)).willReturn(response);

            mockMvc.perform(get("/api/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("Public — inactive product returns 404 Not Found")
        void publicGetInactiveProduct_Returns404() throws Exception {
            given(productService.getPublicProductById(2L))
                    .willThrow(new ProductNotFoundException("Product with ID 2 not found or is not available"));

            mockMvc.perform(get("/api/products/2"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Public — non-existent product returns 404 Not Found")
        void publicGetNonExistentProduct_Returns404() throws Exception {
            given(productService.getPublicProductById(999L))
                    .willThrow(new ProductNotFoundException("Product with ID 999 not found or is not available"));

            mockMvc.perform(get("/api/products/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Public — empty product list returns 200 with empty page")
        void publicListProducts_Empty() throws Exception {
            PageImpl<ProductResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            given(productService.getPublicProducts(isNull(), isNull(), eq(0), eq(20))).willReturn(emptyPage);

            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }
}
