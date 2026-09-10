package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryAnalyticsResponse(
        Long categoryId,
        String categoryName,
        long productCount,
        long orderCount,
        BigDecimal totalRevenue
) {
}
