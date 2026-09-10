package com.shopstack.repository;

import com.shopstack.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ProductRepository — Spring Data JPA repository for the {@link Product} entity.
 *
 * <p>Provides database queries for vendor-scoped operations, admin operations,
 * and public-facing product discovery.
 *
 * <p>Query naming convention:
 * <ul>
 *   <li>{@code findBy*} — retrieves one or many records</li>
 *   <li>{@code existsBy*} — returns boolean for uniqueness checks</li>
 *   <li>{@code findAllActive*} — public-safe queries (active=true only)</li>
 * </ul>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // =========================================================================
    // Vendor-scoped Queries
    // =========================================================================

    /**
     * Returns all products belonging to a specific vendor profile.
     * Used by vendors to list their own products (includes inactive ones).
     *
     * @param vendorProfileId the vendor profile ID
     * @return list of products for that vendor
     */
    List<Product> findByVendorProfileId(Long vendorProfileId);

    /**
     * Finds a single product by ID that belongs to a specific vendor.
     * Used for ownership verification — ensures a vendor can only access their own product.
     *
     * @param id              the product ID
     * @param vendorProfileId the vendor profile ID
     * @return optional product
     */
    Optional<Product> findByIdAndVendorProfileId(Long id, Long vendorProfileId);

    // =========================================================================
    // Uniqueness Checks
    // =========================================================================

    /**
     * Checks if a product with the given SKU already exists.
     * Used during creation to prevent duplicate SKUs.
     *
     * @param sku the stock keeping unit code
     * @return true if any product has this SKU
     */
    boolean existsBySku(String sku);

    /**
     * Checks if a product with the given SKU already exists, excluding a specific product ID.
     * Used during update to allow keeping the same SKU when editing.
     *
     * @param sku the stock keeping unit code
     * @param id  the product ID to exclude
     * @return true if another product has this SKU
     */
    boolean existsBySkuAndIdNot(String sku, Long id);

    /**
     * Checks if a product with the given slug already exists.
     *
     * @param slug the URL-safe slug
     * @return true if any product has this slug
     */
    boolean existsBySlug(String slug);

    /**
     * Checks if a product with the given slug already exists, excluding a specific product ID.
     *
     * @param slug the URL-safe slug
     * @param id   the product ID to exclude
     * @return true if another product has this slug
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    // =========================================================================
    // Public / Active-Only Queries
    // =========================================================================

    /**
     * Returns a paginated list of active products with optional name search and category filter.
     *
     * <p>This is the primary public product discovery query. All inactive products are excluded.
     *
     * <p>JPQL query breakdown:
     * <ul>
     *   <li>{@code p.active = true} — only visible products</li>
     *   <li>{@code LOWER(p.name) LIKE LOWER(:search)} — case-insensitive name search</li>
     *   <li>{@code p.category.id = :categoryId} — optional category filter (null = all)</li>
     * </ul>
     *
     * @param search     partial name to search (use "%term%" format, or "%%" for all)
     * @param categoryId optional category filter, null to return all categories
     * @param pageable   pagination and sorting parameters
     * @return paginated page of active products
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND LOWER(p.name) LIKE LOWER(:search)
              AND (:categoryId IS NULL OR p.category.id = :categoryId OR (p.category.parentCategory IS NOT NULL AND p.category.parentCategory.id = :categoryId))
            ORDER BY p.featured DESC, p.createdAt DESC
            """)
    Page<Product> findActiveProducts(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    /**
     * Returns a single active product by its ID.
     * Used for public product detail pages. Returns empty if inactive.
     *
     * @param id the product ID
     * @return optional active product
     */
    Optional<Product> findByIdAndActiveTrue(Long id);

    /**
     * Returns all active products belonging to a specific category.
     * Useful for category page listings.
     *
     * @param categoryId the category ID
     * @return list of active products in that category
     */
    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}
