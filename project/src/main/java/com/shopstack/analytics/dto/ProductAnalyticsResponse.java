package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductAnalyticsResponse(
        Long productId,
        String productName,
        long totalSold,
        BigDecimal totalRevenue,
        Integer stockQuantity,
        Integer lowStockThreshold,
        String status
) {
}
