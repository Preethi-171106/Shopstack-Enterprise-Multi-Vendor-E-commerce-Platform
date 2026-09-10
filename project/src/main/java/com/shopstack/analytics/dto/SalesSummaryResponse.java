package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalesSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue,
        List<SalesDataPoint> dataPoints
) {
}
