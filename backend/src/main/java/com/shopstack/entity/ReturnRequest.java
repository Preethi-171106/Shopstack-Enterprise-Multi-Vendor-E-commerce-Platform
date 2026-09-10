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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "returns",
        indexes = {
                @Index(name = "idx_returns_return_number", columnList = "return_number", unique = true),
                @Index(name = "idx_returns_order_id", columnList = "order_id"),
                @Index(name = "idx_returns_user_id", columnList = "user_id"),
                @Index(name = "idx_returns_order_item_id", columnList = "order_item_id"),
                @Index(name = "idx_returns_return_wh", columnList = "return_warehouse_id"),
                @Index(name = "idx_returns_qc_status", columnList = "qc_status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_number", nullable = false, unique = true, length = 50)
    private String returnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(name = "return_quantity", nullable = false)
    @Builder.Default
    private int returnQuantity = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Original physical warehouse that fulfilled the item (from OrderItemWarehouseAllocation).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_warehouse_id")
    private Warehouse originalWarehouse;

    /**
     * Destination physical warehouse designated for receiving the return.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_warehouse_id")
    private Warehouse returnWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReturnReason reason;

    @Column(length = 1000)
    private String description;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReturnStatus status;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "return_received_date")
    private LocalDateTime returnReceivedDate;

    @Column(name = "package_condition", length = 100)
    private String packageCondition;

    @Column(name = "receiving_notes", length = 1000)
    private String receivingNotes;

    @Column(name = "qc_status", length = 50)
    @Builder.Default
    private String qcStatus = "PENDING";

    @Enumerated(EnumType.STRING)
    @Column(name = "qc_result", length = 50)
    private QualityCheckResult qcResult;

    @Column(name = "qc_notes", length = 1000)
    private String qcNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_inspector_id")
    private User qcInspector;

    @Column(name = "qc_completed_at")
    private LocalDateTime qcCompletedAt;

    @Column(name = "accepted_quantity", nullable = false)
    @Builder.Default
    private int acceptedQuantity = 0;

    @Column(name = "damaged_quantity", nullable = false)
    @Builder.Default
    private int damagedQuantity = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
