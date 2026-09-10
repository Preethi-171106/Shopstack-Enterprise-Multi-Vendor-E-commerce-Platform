package com.shopstack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShipmentStatusResponse(
        String status,
        long count
) {
}
