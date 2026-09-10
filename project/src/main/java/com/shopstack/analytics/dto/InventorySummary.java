package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InventorySummary(
        long totalProducts,
        long lowStockCount,
        long outOfStockCount,
        long inStockCount
) {
}
