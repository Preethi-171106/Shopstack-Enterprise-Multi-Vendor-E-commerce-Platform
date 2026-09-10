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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * WarehouseInventory — JPA Entity tracking stock levels for a Product at a specific physical Warehouse.
 *
 * <p>Table: {@code warehouse_inventories}
 *
 * <p>Invariant: {@code availableQuantity = totalQuantity - reservedQuantity >= 0}
 */
@Entity
@Table(
        name = "warehouse_inventories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_warehouse_product", columnNames = {"warehouse_id", "product_id"})
        },
        indexes = {
                @Index(name = "idx_wh_inv_warehouse_id", columnList = "warehouse_id"),
                @Index(name = "idx_wh_inv_product_id", columnList = "product_id"),
                @Index(name = "idx_wh_inv_available", columnList = "available_quantity")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private int totalQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private int reservedQuantity = 0;

    @Column(name = "available_quantity", nullable = false)
    @Builder.Default
    private int availableQuantity = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private int lowStockThreshold = 10;

    @Column(name = "damaged_quantity", nullable = false)
    @Builder.Default
    private int damagedQuantity = 0;

    @Column(name = "quarantine_quantity", nullable = false)
    @Builder.Default
    private int quarantineQuantity = 0;

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
        recalculateAvailableQuantity();
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        recalculateAvailableQuantity();
        this.updatedAt = LocalDateTime.now();
    }

    public void recalculateAvailableQuantity() {
        this.availableQuantity = Math.max(0, this.totalQuantity - this.reservedQuantity);
    }

    public boolean isLowStock() {
        return this.totalQuantity <= this.lowStockThreshold;
    }
}
