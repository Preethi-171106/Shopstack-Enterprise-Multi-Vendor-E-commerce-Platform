package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VendorAnalyticsResponse(
        Long vendorId,
        String storeName,
        long productCount,
        long orderCount,
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue
) {
}
