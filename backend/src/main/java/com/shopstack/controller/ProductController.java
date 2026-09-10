package com.shopstack.controller;

import com.shopstack.dto.product.ProductCreateRequest;
import com.shopstack.dto.product.ProductResponse;
import com.shopstack.dto.product.ProductStatusRequest;
import com.shopstack.dto.product.ProductUpdateRequest;
import com.shopstack.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ProductController — REST Controller handling all product endpoints.
 *
 * <p>Three URL namespaces with distinct security requirements:
 * <ul>
 *   <li>{@code /api/vendors/products/**} — ROLE_VENDOR only (enforced in SecurityConfig)</li>
 *   <li>{@code /api/admin/products/**} — ROLE_ADMIN only (enforced in SecurityConfig)</li>
 *   <li>{@code /api/products/**} — Public, no authentication required</li>
 * </ul>
 *
 * <p>Authorization is enforced at the SecurityConfig level (not via @PreAuthorize),
 * consistent with the existing pattern in this project. Vendor ownership is enforced
 * in the service layer by comparing the JWT identity to the product's vendorProfile.
 *
 * <p>This controller is intentionally thin — it delegates all business logic to
 * {@link ProductService} and only handles HTTP concerns (status codes, request parsing).
 */
@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // =========================================================================
    // Vendor Endpoints — ROLE_VENDOR required (enforced in SecurityConfig)
    // =========================================================================

    /**
     * Creates a new product for the authenticated vendor.
     *
     * <p><b>Endpoint:</b> {@code POST /api/vendors/products}
     * <p><b>Access:</b> VENDOR only
     * <p>The vendor identity is resolved from the JWT — not from the request body.
     *
     * @param request the product creation request
     * @return HTTP 201 Created with the created {@link ProductResponse}
     */
    @PostMapping("/api/vendors/products")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns all products belonging to the authenticated vendor.
     *
     * <p><b>Endpoint:</b> {@code GET /api/vendors/products}
     * <p><b>Access:</b> VENDOR only
     * <p>Includes both active and inactive products (vendor sees their full inventory).
     *
     * @return HTTP 200 OK with list of the vendor's products
     */
    @GetMapping("/api/vendors/products")
    public ResponseEntity<List<ProductResponse>> getVendorProducts() {
        List<ProductResponse> products = productService.getVendorProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Returns a single product by ID for the authenticated vendor.
     *
     * <p><b>Endpoint:</b> {@code GET /api/vendors/products/{id}}
     * <p><b>Access:</b> VENDOR only — returns 403 if product belongs to a different vendor
     *
     * @param id the product ID
     * @return HTTP 200 OK with the {@link ProductResponse}
     */
    @GetMapping("/api/vendors/products/{id}")
    public ResponseEntity<ProductResponse> getVendorProductById(@PathVariable Long id) {
        ProductResponse response = productService.getVendorProductById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the authenticated vendor's own product.
     *
     * <p><b>Endpoint:</b> {@code PUT /api/vendors/products/{id}}
     * <p><b>Access:</b> VENDOR only — returns 403 if product belongs to a different vendor
     * <p>Full replacement semantics — all updatable fields must be provided.
     *
     * @param id      the product ID to update
     * @param request the update request
     * @return HTTP 200 OK with the updated {@link ProductResponse}
     */
    @PutMapping("/api/vendors/products/{id}")
    public ResponseEntity<ProductResponse> updateVendorProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResponse response = productService.updateVendorProduct(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes (deactivates) the authenticated vendor's own product.
     *
     * <p><b>Endpoint:</b> {@code DELETE /api/vendors/products/{id}}
     * <p><b>Access:</b> VENDOR only — returns 403 if product belongs to a different vendor
     * <p>This is a soft delete — sets {@code active = false} rather than removing the record.
     *
     * @param id the product ID to deactivate
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/api/vendors/products/{id}")
    public ResponseEntity<Void> deleteVendorProduct(@PathVariable Long id) {
        productService.deleteVendorProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggles the active status of the authenticated vendor's own product.
     *
     * <p><b>Endpoint:</b> {@code PATCH /api/vendors/products/{id}/status}
     * <p><b>Access:</b> VENDOR only
     * <p>Accepts: {@code {"active": true}} or {@code {"active": false}}
     *
     * @param id      the product ID
     * @param request the status request with new active value
     * @return HTTP 200 OK with updated {@link ProductResponse}
     */
    @PatchMapping("/api/vendors/products/{id}/status")
    public ResponseEntity<ProductResponse> updateVendorProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusRequest request
    ) {
        ProductResponse response = productService.updateVendorProductStatus(id, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Admin Endpoints — ROLE_ADMIN required (enforced in SecurityConfig)
    // =========================================================================

    /**
     * Returns all products across all vendors (admin view).
     *
     * <p><b>Endpoint:</b> {@code GET /api/admin/products}
     * <p><b>Access:</b> ADMIN only
     * <p>Includes both active and inactive products from all vendors.
     *
     * @return HTTP 200 OK with list of all products
     */
    @GetMapping("/api/admin/products")
    public ResponseEntity<List<ProductResponse>> getAllProductsForAdmin() {
        List<ProductResponse> products = productService.getAllProductsForAdmin();
        return ResponseEntity.ok(products);
    }

    /**
     * Returns a single product by ID for admin viewing (no ownership restriction).
     *
     * <p><b>Endpoint:</b> {@code GET /api/admin/products/{id}}
     * <p><b>Access:</b> ADMIN only
     *
     * @param id the product ID
     * @return HTTP 200 OK with the {@link ProductResponse}
     */
    @GetMapping("/api/admin/products/{id}")
    public ResponseEntity<ProductResponse> getAdminProductById(@PathVariable Long id) {
        ProductResponse response = productService.getAdminProductById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates any product as admin (no ownership restriction).
     *
     * <p><b>Endpoint:</b> {@code PUT /api/admin/products/{id}}
     * <p><b>Access:</b> ADMIN only
     *
     * @param id      the product ID to update
     * @param request the update request
     * @return HTTP 200 OK with the updated {@link ProductResponse}
     */
    @PutMapping("/api/admin/products/{id}")
    public ResponseEntity<ProductResponse> updateAdminProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResponse response = productService.updateAdminProduct(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Toggles the active status of any product as admin.
     *
     * <p><b>Endpoint:</b> {@code PATCH /api/admin/products/{id}/status}
     * <p><b>Access:</b> ADMIN only
     *
     * @param id      the product ID
     * @param request the status request with new active value
     * @return HTTP 200 OK with updated {@link ProductResponse}
     */
    @PatchMapping("/api/admin/products/{id}/status")
    public ResponseEntity<ProductResponse> updateAdminProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusRequest request
    ) {
        ProductResponse response = productService.updateAdminProductStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes any product as admin.
     *
     * <p><b>Endpoint:</b> {@code DELETE /api/admin/products/{id}}
     * <p><b>Access:</b> ADMIN only
     *
     * @param id the product ID to deactivate
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/api/admin/products/{id}")
    public ResponseEntity<Void> deleteAdminProduct(@PathVariable Long id) {
        productService.deleteAdminProduct(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Public Endpoints — No authentication required
    // =========================================================================

    /**
     * Returns a paginated list of active products with optional search and category filter.
     *
     * <p><b>Endpoint:</b> {@code GET /api/products}
     * <p><b>Access:</b> Public (no JWT required)
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code search} — optional partial product name search</li>
     *   <li>{@code categoryId} — optional category ID filter</li>
     *   <li>{@code page} — zero-based page number (default 0)</li>
     *   <li>{@code size} — page size (default 20, max 100)</li>
     * </ul>
     *
     * @param search     optional product name search term
     * @param categoryId optional category ID filter
     * @param page       page number (default 0)
     * @param size       page size (default 20)
     * @return HTTP 200 OK with paginated list of active {@link ProductResponse} DTOs
     */
    @GetMapping("/api/products")
    public ResponseEntity<Page<ProductResponse>> getPublicProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ProductResponse> products = productService.getPublicProducts(search, categoryId, page, size);
        return ResponseEntity.ok(products);
    }

    /**
     * Returns a single active product by ID for public viewing.
     *
     * <p><b>Endpoint:</b> {@code GET /api/products/{id}}
     * <p><b>Access:</b> Public (no JWT required)
     * <p>Returns HTTP 404 if the product does not exist or is inactive.
     *
     * @param id the product ID
     * @return HTTP 200 OK with the {@link ProductResponse}
     */
    @GetMapping("/api/products/{id}")
    public ResponseEntity<ProductResponse> getPublicProductById(@PathVariable Long id) {
        ProductResponse response = productService.getPublicProductById(id);
        return ResponseEntity.ok(response);
    }
}
