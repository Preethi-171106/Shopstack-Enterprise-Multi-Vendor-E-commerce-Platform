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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * OrderItemWarehouseAllocation — Links an individual OrderItem to the designated physical Warehouse
 * responsible for fulfillment, tracking the line item's lifecycle:
 * ALLOCATED → PICKED → PACKED → READY_FOR_SHIPMENT.
 */
@Entity
@Table(
        name = "order_item_warehouse_allocations",
        indexes = {
                @Index(name = "idx_alloc_order_item_id", columnList = "order_item_id"),
                @Index(name = "idx_alloc_warehouse_id", columnList = "warehouse_id"),
                @Index(name = "idx_alloc_status", columnList = "allocation_status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemWarehouseAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_inventory_id")
    private WarehouseInventory warehouseInventory;

    @Column(name = "allocated_quantity", nullable = false)
    private int allocatedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_status", nullable = false, length = 50)
    @Builder.Default
    private StockAllocationStatus allocationStatus = StockAllocationStatus.ALLOCATED;

    @Column(name = "allocated_at", nullable = false)
    private LocalDateTime allocatedAt;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "packed_at")
    private LocalDateTime packedAt;

    @Column(name = "ready_for_shipment_at")
    private LocalDateTime readyForShipmentAt;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.allocatedAt == null) {
            this.allocatedAt = LocalDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.allocationStatus == null) {
            this.allocationStatus = StockAllocationStatus.ALLOCATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
