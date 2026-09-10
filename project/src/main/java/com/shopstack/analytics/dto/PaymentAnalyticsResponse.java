package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentAnalyticsResponse(
        String method,
        long count,
        BigDecimal totalAmount,
        String percentage
) {
}
