package com.shopstack.service;

import com.shopstack.dto.product.ProductCreateRequest;
import com.shopstack.dto.product.ProductResponse;
import com.shopstack.dto.product.ProductStatusRequest;
import com.shopstack.dto.product.ProductUpdateRequest;
import com.shopstack.entity.Category;
import com.shopstack.entity.Product;
import com.shopstack.entity.VendorProfile;
import com.shopstack.exception.CategoryNotFoundException;
import com.shopstack.exception.ProductAlreadyExistsException;
import com.shopstack.exception.ProductNotFoundException;
import com.shopstack.exception.ProductOwnershipException;
import com.shopstack.exception.VendorProfileNotFoundException;
import com.shopstack.mapper.ProductMapper;
import com.shopstack.repository.CategoryRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.VendorProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * ProductService — Business service managing the full product lifecycle.
 *
 * <h2>Architecture</h2>
 * <p>This service handles three audiences:
 * <ul>
 *   <li><b>Vendors</b> — create and manage only their own products</li>
 *   <li><b>Admins</b> — view and manage all products across all vendors</li>
 *   <li><b>Public</b> — browse only active products, with search and category filter</li>
 * </ul>
 *
 * <h2>Security Design</h2>
 * <p>Vendor identity is always extracted from the Spring Security context
 * (populated by the JWT filter during request processing). The vendor ID is never
 * accepted as a client-supplied parameter — this prevents ownership spoofing.
 *
 * <h2>Soft Delete</h2>
 * <p>Products are never hard-deleted. Setting {@code active = false} hides a product
 * from public APIs while preserving it for order history and audit.
 *
 * <h2>Slug Generation</h2>
 * <p>Slugs are auto-generated from the product name using the same algorithm as
 * {@link CategoryService}. Slug conflicts are resolved with a numeric suffix.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductService(
            ProductRepository productRepository,
            VendorProfileRepository vendorProfileRepository,
            CategoryRepository categoryRepository,
            ProductMapper productMapper
    ) {
        this.productRepository = productRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    // =========================================================================
    // Vendor Operations
    // =========================================================================

    /**
     * Creates a new product for the currently authenticated vendor.
     *
     * <p>The vendor's profile is resolved from the JWT. The client cannot supply
     * or spoof the vendor identity. Business rules enforced:
     * <ul>
     *   <li>SKU must be globally unique</li>
     *   <li>Slug auto-generated from name and must be unique</li>
     *   <li>Category must exist</li>
     *   <li>Vendor profile must exist for the authenticated user</li>
     *   <li>originalPrice must not be less than price, if provided</li>
     * </ul>
     *
     * @param request the product creation request (no vendor ID — resolved from JWT)
     * @return the created {@link ProductResponse}
     */
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        VendorProfile vendorProfile = resolveAuthenticatedVendorProfile();
        Category category = resolveCategory(request.categoryId());

        String trimmedName = request.name().trim();
        String sku = request.sku().trim().toUpperCase();
        String slug = generateUniqueSlug(trimmedName, null);

        // SKU uniqueness check
        if (productRepository.existsBySku(sku)) {
            throw new ProductAlreadyExistsException(
                    "A product with SKU '" + sku + "' already exists"
            );
        }

        // Business rule: originalPrice should not be less than price
        validateOriginalPrice(request.price(), request.originalPrice());

        Product product = Product.builder()
                .vendorProfile(vendorProfile)
                .category(category)
                .name(trimmedName)
                .slug(slug)
                .description(request.description() != null ? request.description().trim() : null)
                .price(request.price())
                .originalPrice(request.originalPrice())
                .stockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0)
                .sku(sku)
                .imageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null)
                .active(true)
                .featured(false)
                .build();

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    /**
     * Returns all products belonging to the authenticated vendor (including inactive).
     *
     * @return list of all products for this vendor
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getVendorProducts() {
        VendorProfile vendorProfile = resolveAuthenticatedVendorProfile();
        return productRepository.findByVendorProfileId(vendorProfile.getId())
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    /**
     * Returns a single product by ID, enforcing vendor ownership.
     *
     * @param productId the product ID
     * @return the {@link ProductResponse} if the product belongs to this vendor
     * @throws ProductNotFoundException   if no product exists with this ID
     * @throws ProductOwnershipException  if the product belongs to a different vendor
     */
    @Transactional(readOnly = true)
    public ProductResponse getVendorProductById(Long productId) {
        VendorProfile vendorProfile = resolveAuthenticatedVendorProfile();
        Product product = findProductById(productId);
        assertOwnership(product, vendorProfile);
        return productMapper.toResponse(product);
    }

    /**
     * Updates a vendor's own product by ID.
     *
     * <p>Vendor ownership is strictly enforced — a vendor cannot update a product
     * owned by another vendor even if they know the product ID.
     * Slug is regenerated if the name changes. SKU must remain unique.
     *
     * @param productId the product ID to update
     * @param request   the update request
     * @return the updated {@link ProductResponse}
     * @throws ProductNotFoundException   if the product does not exist
     * @throws ProductOwnershipException  if the vendor does not own this product
     * @throws ProductAlreadyExistsException if the new SKU conflicts
     */
    @Transactional
    public ProductResponse updateVendorProduct(Long productId, ProductUpdateRequest request) {
        VendorProfile vendorProfile = resolveAuthenticatedVendorProfile();
        Product product = findProductById(productId);
        assertOwnership(product, vendorProfile);

        return applyProductUpdate(product, request);
    }

    /**
     * Soft-deletes a vendor's own product by setting {@code active = false}.
     *
     * <p>The product is not removed from the database — it is simply hidden from
     * public listings. This preserves order history integrity.
     *
     * @param productId the product ID to deactivate
     * @throws ProductNotFoundException   if the product does not exist
     * @throws ProductOwnershipException  if the vendor does not own this product
     */
    @Transactional
    public void deleteVendorProduct(Long productId) {
        VendorProfile vendorProfile = resolveAuthenticatedVendorProfile();
        Product product = findProductById(productId);
        assertOwnership(product, vendorProfile);

        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * Toggles the active status of a vendor's own product.
     *
     * @param productId the product ID
     * @param request   the status request with the new active value
     * @return the updated {@link ProductResponse}
     * @throws ProductNotFoundException   if the product does not exist
     * @throws ProductOwnershipException  if the vendor does not own this product
     */
    @Transactional
    public ProductResponse updateVendorProductStatus(Long productId, ProductStatusRequest request) {
        VendorProfile vendorProfile = resolveAuthenticatedVendorProfile();
        Product product = findProductById(productId);
        assertOwnership(product, vendorProfile);

        product.setActive(request.active());
        Product updated = productRepository.save(product);
        return productMapper.toResponse(updated);
    }

    // =========================================================================
    // Admin Operations
    // =========================================================================

    /**
     * Returns all products across all vendors (admin view, includes inactive).
     *
     * @return list of all products
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProductsForAdmin() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    /**
     * Returns a single product by ID for admin viewing (no ownership restriction).
     *
     * @param productId the product ID
     * @return the {@link ProductResponse}
     * @throws ProductNotFoundException if the product does not exist
     */
    @Transactional(readOnly = true)
    public ProductResponse getAdminProductById(Long productId) {
        Product product = findProductById(productId);
        return productMapper.toResponse(product);
    }

    /**
     * Updates any product as an admin (no ownership restriction).
     *
     * @param productId the product ID to update
     * @param request   the update request
     * @return the updated {@link ProductResponse}
     * @throws ProductNotFoundException      if the product does not exist
     * @throws ProductAlreadyExistsException if the new SKU conflicts
     */
    @Transactional
    public ProductResponse updateAdminProduct(Long productId, ProductUpdateRequest request) {
        Product product = findProductById(productId);
        return applyProductUpdate(product, request);
    }

    /**
     * Toggles the active status of any product as an admin.
     *
     * @param productId the product ID
     * @param request   the status request
     * @return the updated {@link ProductResponse}
     * @throws ProductNotFoundException if the product does not exist
     */
    @Transactional
    public ProductResponse updateAdminProductStatus(Long productId, ProductStatusRequest request) {
        Product product = findProductById(productId);
        product.setActive(request.active());
        Product updated = productRepository.save(product);
        return productMapper.toResponse(updated);
    }

    /**
     * Soft-deletes any product as an admin.
     *
     * @param productId the product ID to deactivate
     * @throws ProductNotFoundException if the product does not exist
     */
    @Transactional
    public void deleteAdminProduct(Long productId) {
        Product product = findProductById(productId);
        product.setActive(false);
        productRepository.save(product);
    }

    // =========================================================================
    // Public Operations
    // =========================================================================

    /**
     * Returns a paginated list of active products with optional name search and category filter.
     *
     * <p>This is the primary public product discovery endpoint. Inactive products are
     * never returned, regardless of search parameters.
     *
     * @param search     partial product name to search (null or blank = all products)
     * @param categoryId optional category filter (null = all categories)
     * @param page       zero-based page number (default 0)
     * @param size       page size (default 20, max 100)
     * @return paginated page of active {@link ProductResponse} DTOs
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublicProducts(String search, Long categoryId, int page, int size) {
        // Clamp page size to prevent abuse
        int clampedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, clampedSize);

        // Build the LIKE pattern for name search
        String searchPattern = (search == null || search.isBlank()) ? "%%" : "%" + search.trim() + "%";

        return productRepository.findActiveProducts(searchPattern, categoryId, pageable)
                .map(productMapper::toResponse);
    }

    /**
     * Returns a single active product by ID for public viewing.
     *
     * <p>Returns 404 if the product does not exist or is inactive.
     *
     * @param productId the product ID
     * @return the {@link ProductResponse} for the active product
     * @throws ProductNotFoundException if the product is missing or inactive
     */
    @Transactional(readOnly = true)
    public ProductResponse getPublicProductById(Long productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with ID " + productId + " not found or is not available"
                ));
        return productMapper.toResponse(product);
    }

    // =========================================================================
    // Internal Helpers
    // =========================================================================

    /**
     * Extracts the authenticated user's email from the Spring Security context
     * and resolves it to a {@link VendorProfile}.
     *
     * <p>This is the single source of truth for vendor identity in the service layer.
     * The email comes from the JWT token set by {@code JwtAuthenticationFilter} —
     * it cannot be forged by the client.
     *
     * @return the {@link VendorProfile} for the currently authenticated vendor
     * @throws VendorProfileNotFoundException if the authenticated user has no vendor profile
     */
    private VendorProfile resolveAuthenticatedVendorProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        // Use case-insensitive lookup to guard against any email casing inconsistency
        // between what was stored at registration and what the JWT subject contains.
        return vendorProfileRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new VendorProfileNotFoundException(
                        "No vendor profile found for authenticated user with email: " + email
                ));
    }

    /**
     * Fetches a category by ID, throwing 404 if it does not exist.
     *
     * @param categoryId the category ID
     * @return the {@link Category}
     * @throws CategoryNotFoundException if the category does not exist
     */
    private Category resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with ID " + categoryId + " not found"
                ));
    }

    /**
     * Fetches a product by ID, throwing 404 if it does not exist.
     *
     * @param productId the product ID
     * @return the {@link Product}
     * @throws ProductNotFoundException if the product does not exist
     */
    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with ID " + productId + " not found"
                ));
    }

    /**
     * Asserts that the given product belongs to the given vendor profile.
     *
     * <p>Uses the product's vendorProfile ID, not the full entity, to avoid
     * unnecessary lazy loading.
     *
     * @param product       the product to check
     * @param vendorProfile the authenticated vendor's profile
     * @throws ProductOwnershipException if the product belongs to a different vendor
     */
    private void assertOwnership(Product product, VendorProfile vendorProfile) {
        if (!product.getVendorProfile().getId().equals(vendorProfile.getId())) {
            throw new ProductOwnershipException(
                    "Access denied: you do not own product with ID " + product.getId()
            );
        }
    }

    /**
     * Applies the fields from a {@link ProductUpdateRequest} to a {@link Product} entity.
     *
     * <p>Shared by both vendor update and admin update paths. Handles:
     * <ul>
     *   <li>Name trim and slug regeneration</li>
     *   <li>Category lookup and validation</li>
     *   <li>SKU uniqueness check (excluding current product)</li>
     *   <li>originalPrice vs price validation</li>
     * </ul>
     *
     * @param product the product entity to update
     * @param request the update request
     * @return the saved and mapped {@link ProductResponse}
     */
    private ProductResponse applyProductUpdate(Product product, ProductUpdateRequest request) {
        String trimmedName = request.name().trim();
        String sku = request.sku().trim().toUpperCase();
        String newSlug = generateUniqueSlug(trimmedName, product.getId());

        Category category = resolveCategory(request.categoryId());

        // SKU uniqueness check (excluding current product)
        if (productRepository.existsBySkuAndIdNot(sku, product.getId())) {
            throw new ProductAlreadyExistsException(
                    "A product with SKU '" + sku + "' already exists"
            );
        }

        // Business rule: originalPrice should not be less than price
        validateOriginalPrice(request.price(), request.originalPrice());

        product.setName(trimmedName);
        product.setSlug(newSlug);
        product.setCategory(category);
        product.setDescription(request.description() != null ? request.description().trim() : null);
        product.setPrice(request.price());
        product.setOriginalPrice(request.originalPrice());
        product.setStockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0);
        product.setSku(sku);
        product.setImageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null);

        Product updated = productRepository.save(product);
        return productMapper.toResponse(updated);
    }

    /**
     * Validates that {@code originalPrice}, if provided, is not less than {@code price}.
     *
     * <p>This reflects a common business rule: "original price" represents the
     * list/retail price before any discount, so it should always be ≥ the selling price.
     *
     * @param price         the selling price
     * @param originalPrice the original list price (may be null)
     * @throws IllegalArgumentException if originalPrice is less than price
     */
    private void validateOriginalPrice(BigDecimal price, BigDecimal originalPrice) {
        if (originalPrice != null && price != null && originalPrice.compareTo(price) < 0) {
            throw new IllegalArgumentException(
                    "Original price (" + originalPrice + ") cannot be less than selling price (" + price + ")"
            );
        }
    }

    /**
     * Generates a URL-safe slug from a product name, ensuring global uniqueness.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Lowercase the name</li>
     *   <li>Replace non-alphanumeric characters with hyphens</li>
     *   <li>Strip leading/trailing hyphens</li>
     *   <li>If slug exists (and doesn't belong to {@code excludeId}), append {@code -2}, {@code -3}, etc.</li>
     * </ol>
     *
     * @param name      the product name
     * @param excludeId the product ID to exclude from uniqueness check (null for new products)
     * @return a unique URL-safe slug
     */
    String generateUniqueSlug(String name, Long excludeId) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        String candidate = base;
        int counter = 2;

        while (true) {
            boolean conflict = (excludeId == null)
                    ? productRepository.existsBySlug(candidate)
                    : productRepository.existsBySlugAndIdNot(candidate, excludeId);

            if (!conflict) {
                return candidate;
            }
            candidate = base + "-" + counter;
            counter++;
        }
    }
}
