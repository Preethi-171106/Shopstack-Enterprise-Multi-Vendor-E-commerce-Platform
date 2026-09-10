package com.shopstack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product — JPA Entity representing a product listed by a vendor in PostgreSQL.
 *
 * <p>Table: {@code products}
 *
 * <p>Relationships:
 * <ul>
 *   <li>Many products belong to one {@link VendorProfile} (via {@code vendor_profile_id})</li>
 *   <li>Many products belong to one {@link Category} (via {@code category_id})</li>
 * </ul>
 *
 * <p>Security note: The {@code vendorProfile} field is always set from the authenticated
 * JWT identity in the service layer — clients can never supply it directly.
 *
 * <p>Slug is auto-generated from the product name. SKU must be globally unique.
 */
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_vendor_profile_id", columnList = "vendor_profile_id"),
                @Index(name = "idx_products_category_id", columnList = "category_id"),
                @Index(name = "idx_products_sku", columnList = "sku", unique = true),
                @Index(name = "idx_products_slug", columnList = "slug", unique = true),
                @Index(name = "idx_products_active", columnList = "active")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The vendor who listed this product. Resolved from the JWT — never client-supplied.
     * Loaded lazily to avoid N+1 queries in list endpoints.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_profile_id", nullable = false)
    private VendorProfile vendorProfile;

    /**
     * The product category (e.g., Electronics, Home & Kitchen).
     * Loaded eagerly since it is always included in responses.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Human-readable product name. Required, max 200 chars.
     * Example: "Apple iPhone 15 Pro Max 256GB"
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * URL-safe identifier auto-generated from name.
     * Example: "apple-iphone-15-pro-max-256gb"
     * Globally unique, max 250 chars.
     */
    @Column(nullable = false, unique = true, length = 250)
    private String slug;

    /**
     * Optional detailed product description.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Selling price in the store's default currency.
     * Must be non-negative. Uses DECIMAL(10,2) for precision.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Optional original/list price used to show a strikethrough price.
     * If present, should not be less than the actual {@code price}.
     */
    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    /**
     * Quantity available in stock. Cannot be negative. Defaults to 0.
     */
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    /**
     * Stock Keeping Unit — a unique identifier for inventory tracking.
     * Required and globally unique across all products.
     * Example: "APPLE-IP15-256-BLK"
     */
    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    /**
     * Optional URL pointing to the primary product image.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Whether the product is visible to public users.
     * Defaults to {@code true}. Toggled via PATCH status endpoint (soft delete).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Whether the product is highlighted as a featured item.
     * Admins may use this to promote certain products.
     * Defaults to {@code false}.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean featured = false;

    /**
     * Average customer rating on a 0.0 to 5.0 scale.
     * Populated from the review system. Defaults to 0.0.
     */
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    /**
     * Total number of customer reviews for this product.
     * Defaults to 0.
     */
    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Sets default timestamp values before initial persist if not already set.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.rating == null) {
            this.rating = BigDecimal.ZERO;
        }
    }

    /**
     * Updates the updatedAt timestamp before every update.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
