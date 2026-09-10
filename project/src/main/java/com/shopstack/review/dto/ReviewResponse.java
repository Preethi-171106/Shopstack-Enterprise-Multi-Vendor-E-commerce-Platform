package com.shopstack.review.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewResponse(
        Long id,
        Long userId,
        String userFullName,
        Long productId,
        String productName,
        Long orderId,
        Integer rating,
        String title,
        String comment,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
