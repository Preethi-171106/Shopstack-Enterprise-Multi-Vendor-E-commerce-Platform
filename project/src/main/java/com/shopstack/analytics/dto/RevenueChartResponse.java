package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RevenueChartResponse(
        List<ChartPoint> points,
        BigDecimal totalRevenue
) {
}
