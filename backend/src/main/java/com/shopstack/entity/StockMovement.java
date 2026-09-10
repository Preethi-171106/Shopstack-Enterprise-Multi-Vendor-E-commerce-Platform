package com.shopstack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * StockMovement — JPA Entity tracking immutable audit log entries for inventory changes.
 *
 * <p>Table: {@code stock_movements}
 *
 * <p>Records every stock change operation (IN, OUT, ADJUSTMENT, RESERVED, RELEASED, RETURN),
 * retaining historical total stock snapshots, reference order/shipment IDs, notes, and the user who executed the operation.
 */
@Entity
@Table(
        name = "stock_movements",
        indexes = {
                @Index(name = "idx_stock_movements_product_id", columnList = "product_id"),
                @Index(name = "idx_stock_movements_created_at", columnList = "created_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The product whose stock level was changed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Type of movement (IN, OUT, ADJUSTMENT, RESERVED, RELEASED, RETURN).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private StockMovementType movementType;

    /**
     * The quantity involved in this operation (always positive for change magnitude).
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Snapshot of total stock before this movement occurred.
     */
    @Column(name = "previous_stock", nullable = false)
    private int previousStock;

    /**
     * Snapshot of total stock after this movement occurred.
     */
    @Column(name = "new_stock", nullable = false)
    private int newStock;

    /**
     * Optional reference ID (e.g. Order ID, Shipment tracking #, Supplier Invoice #).
     */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    /**
     * Audit notes or reason for adjustment/movement.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * The physical warehouse where the stock movement occurred (null for legacy global movements).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    /**
     * The user who performed this stock movement (Warehouse Staff, Admin, System).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id")
    private User performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
