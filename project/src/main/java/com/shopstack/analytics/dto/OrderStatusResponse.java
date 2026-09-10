package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderStatusResponse(
        String status,
        long count
) {
}
