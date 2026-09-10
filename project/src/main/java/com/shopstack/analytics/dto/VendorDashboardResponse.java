package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VendorDashboardResponse(
        String storeName,
        long totalProducts,
        long activeProducts,
        long totalOrders,
        long pendingOrders,
        long deliveredOrders,
        BigDecimal revenue,
        BigDecimal monthlyRevenue,
        BigDecimal todayRevenue,
        List<ProductAnalyticsResponse> topSellingProducts,
        List<ProductAnalyticsResponse> lowStockProducts,
        InventorySummary inventorySummary
) {
}
