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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Commission — records the platform fee charged to a vendor for each order item sold.
 *
 * <p>A Commission record is created for every OrderItem when an order is placed.
 * The commission rate is a percentage of the item's actual sale price.
 */
@Entity
@Table(
        name = "commissions",
        indexes = {
                @Index(name = "idx_commissions_vendor_profile_id", columnList = "vendor_profile_id"),
                @Index(name = "idx_commissions_order_item_id",     columnList = "order_item_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_profile_id", nullable = false)
    private VendorProfile vendorProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    /** Gross sale amount for this order item (unit price × quantity). */
    @Column(name = "sale_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal saleAmount;

    /** Commission rate applied (e.g. 0.05 = 5%). */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    /** Actual commission charged = saleAmount × commissionRate. */
    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    /** Net amount payable to vendor = saleAmount − commissionAmount. */
    @Column(name = "vendor_net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal vendorNetAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CommissionStatus status = CommissionStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
