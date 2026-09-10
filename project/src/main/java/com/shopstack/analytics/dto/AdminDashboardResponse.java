package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminDashboardResponse(
        long totalCustomers,
        long totalVendors,
        long totalProducts,
        long totalCategories,
        long totalOrders,
        BigDecimal totalRevenue,
        long totalPayments,
        long pendingOrders,
        long deliveredOrders,
        long cancelledOrders,
        long returnedOrders,
        long totalCoupons,
        long activeCoupons,
        long lowStockProducts,
        long outOfStockProducts
) {
}
