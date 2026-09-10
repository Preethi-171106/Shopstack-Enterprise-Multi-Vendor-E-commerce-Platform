package com.shopstack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Category — JPA Entity representing a product category in PostgreSQL.
 *
 * <p>Table: {@code categories}
 * <p>Categories group products for browsing and navigation. Only active categories
 * are visible to public users. Admins manage the full lifecycle.
 *
 * <p>Slug is auto-generated from the name (e.g., "Home &amp; Kitchen" → "home-kitchen").
 * Both name and slug enforce case-insensitive uniqueness.
 */
@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable category name. Required, unique (case-insensitive), max 100 chars.
     * Example: "Home & Kitchen", "Electronics".
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * URL-safe identifier auto-generated from name.
     * Example: "home-kitchen", "electronics".
     * Unique, max 120 chars.
     */
    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    /**
     * Optional description of the category. Max 1000 chars.
     */
    @Column(length = 1000)
    private String description;

    /**
     * Optional URL pointing to a category banner or icon image.
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Whether the category is visible to public users.
     * Defaults to {@code true} on creation. Toggled via PATCH status endpoint.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory")
    @Builder.Default
    private List<Category> subcategories = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Sets default values before initial persist if not already set.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
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
