package com.shopstack.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AdminDashboardStats — aggregate statistics shown on the admin analytics dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStats {

    // Users
    private long totalCustomers;
    private long totalVendors;
    private long pendingVendorApprovals;
    private long totalWarehouseStaff;

    // Products & Categories
    private long totalProducts;
    private long activeProducts;
    private long totalCategories;

    // Orders
    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long processingOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long returnRequestedOrders;

    // Revenue & Commissions
    private BigDecimal totalRevenue;
    private BigDecimal totalPlatformCommission;
    private BigDecimal totalVendorPayouts;

    // Inventory
    private long lowStockProducts;
    private long inventoryRecords;

    // Returns
    private long totalReturnRequests;
    private long pendingReturnRequests;
}
