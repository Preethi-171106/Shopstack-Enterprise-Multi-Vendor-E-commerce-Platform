package com.shopstack.review.dto;

import jakarta.validation.constraints.*;

public record CreateReviewRequest(
        @NotNull(message = "productId is required")
        Long productId,

        @NotNull(message = "orderId is required")
        Long orderId,

        @NotNull(message = "rating is required")
        @Min(value = 1, message = "rating must be at least 1")
        @Max(value = 5, message = "rating must be at most 5")
        Integer rating,

        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        @NotBlank(message = "comment is required")
        @Size(max = 2000, message = "comment must be at most 2000 characters")
        String comment
) {
}
