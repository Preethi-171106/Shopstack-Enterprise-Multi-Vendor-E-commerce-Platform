package com.shopstack.review.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewSummaryResponse(
        Long productId,
        String productName,
        Double averageRating,
        Long totalReviews,
        Long fiveStarCount,
        Long fourStarCount,
        Long threeStarCount,
        Long twoStarCount,
        Long oneStarCount
) {
}
