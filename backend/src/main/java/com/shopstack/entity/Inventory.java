package com.shopstack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Inventory — JPA Entity tracking real-time stock levels for a product.
 *
 * <p>Table: {@code inventories}
 *
 * <p>Key invariant: {@code availableStock = totalStock - reservedStock >= 0}
 *
 * <p>Uses {@code @Version} for optimistic locking to prevent concurrent update races
 * when multiple warehouse staff modify stock simultaneously.
 *
 * <p>Relationships:
 * <ul>
 *   <li>One inventory per product: {@code @OneToOne(Product)}</li>
 * </ul>
 */
@Entity
@Table(
        name = "inventories",
        indexes = {
                @Index(name = "idx_inventories_product_id", columnList = "product_id", unique = true)
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The product this inventory record tracks. One-to-one, unique.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    /**
     * Total physical stock units (on-hand). Cannot be negative.
     */
    @Column(name = "total_stock", nullable = false)
    @Builder.Default
    private int totalStock = 0;

    /**
     * Units currently reserved for placed orders awaiting fulfilment. Cannot be negative.
     */
    @Column(name = "reserved_stock", nullable = false)
    @Builder.Default
    private int reservedStock = 0;

    /**
     * Available stock = totalStock - reservedStock. Recalculated before every save.
     * This is the quantity that can still be added to cart.
     */
    @Column(name = "available_stock", nullable = false)
    @Builder.Default
    private int availableStock = 0;

    /**
     * Units threshold below which the product is flagged as low-stock.
     * Defaults to 10.
     */
    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private int lowStockThreshold = 10;

    /**
     * Optimistic lock version counter. Prevents lost-update races under concurrent requests.
     */
    @Version
    @Column(nullable = false)
    @Builder.Default
    private int version = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        recalculateAvailableStock();
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        recalculateAvailableStock();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Recalculates availableStock as totalStock - reservedStock, floored at 0.
     * Called automatically before every persist/update.
     */
    public void recalculateAvailableStock() {
        this.availableStock = Math.max(0, this.totalStock - this.reservedStock);
    }

    /**
     * Returns true when totalStock is at or below the lowStockThreshold.
     */
    public boolean isLowStock() {
        return this.totalStock <= this.lowStockThreshold;
    }
}
