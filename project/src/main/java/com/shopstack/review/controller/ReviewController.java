package com.shopstack.review.controller;

import com.shopstack.review.dto.CreateReviewRequest;
import com.shopstack.review.dto.ReviewResponse;
import com.shopstack.review.dto.ReviewSummaryResponse;
import com.shopstack.review.dto.UpdateReviewRequest;
import com.shopstack.review.service.ReviewService;
import com.shopstack.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    public ReviewController(ReviewService reviewService, SecurityUtils securityUtils) {
        this.reviewService = reviewService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateReviewRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(reviewService.updateReview(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        reviewService.deleteReview(userId, id);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(reviewService.getMyReviews(userId, pageable));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(@PathVariable Long productId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId, pageable));
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ReviewSummaryResponse> getProductReviewSummary(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviewSummary(productId));
    }
}
