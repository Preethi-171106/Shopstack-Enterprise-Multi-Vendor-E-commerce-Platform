package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrdersChartResponse(
        List<ChartPoint> points,
        long totalOrders
) {
}
