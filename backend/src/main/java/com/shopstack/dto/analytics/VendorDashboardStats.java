package com.shopstack.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * VendorDashboardStats — aggregate statistics shown on the vendor analytics dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorDashboardStats {

    private long totalProducts;
    private long activeProducts;
    private long lowStockProducts;
    private long totalOrders;
    private long pendingOrders;
    private long deliveredOrders;
    private BigDecimal totalSales;
    private BigDecimal totalCommissionPaid;
    private BigDecimal netRevenue;
}
