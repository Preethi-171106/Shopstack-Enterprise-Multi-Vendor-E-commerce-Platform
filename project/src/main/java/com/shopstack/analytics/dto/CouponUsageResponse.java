package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CouponUsageResponse(
        String code,
        String description,
        String discountType,
        BigDecimal discountValue,
        String status,
        int usedCount,
        Integer maxUses
) {
}
