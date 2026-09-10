package com.shopstack.review.mapper;

import com.shopstack.review.dto.ReviewResponse;
import com.shopstack.review.entity.Review;

public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewResponse toResponse(Review review) {
        String fullName = null;
        if (review.getUser() != null) {
            fullName = review.getUser().getFirstName() + " " + review.getUser().getLastName();
        }

        return new ReviewResponse(
                review.getId(),
                review.getUser() != null ? review.getUser().getId() : null,
                fullName,
                review.getProduct() != null ? review.getProduct().getId() : null,
                review.getProduct() != null ? review.getProduct().getName() : null,
                review.getOrder() != null ? review.getOrder().getId() : null,
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getStatus() != null ? review.getStatus().name() : null,
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
