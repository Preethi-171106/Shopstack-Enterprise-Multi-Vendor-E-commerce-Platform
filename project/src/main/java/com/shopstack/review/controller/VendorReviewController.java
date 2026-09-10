package com.shopstack.review.controller;

import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.common.exception.UnauthorizedActionException;
import com.shopstack.review.dto.ReviewResponse;
import com.shopstack.review.dto.ReviewSummaryResponse;
import com.shopstack.review.service.ReviewService;
import com.shopstack.security.SecurityUtils;
import com.shopstack.vendor.entity.Vendor;
import com.shopstack.vendor.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor/reviews")
public class VendorReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;
    private final VendorRepository vendorRepository;

    public VendorReviewController(ReviewService reviewService,
                                   SecurityUtils securityUtils,
                                   VendorRepository vendorRepository) {
        this.reviewService = reviewService;
        this.securityUtils = securityUtils;
        this.vendorRepository = vendorRepository;
    }

    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getReviewsForOwnProducts(Pageable pageable) {
        Long vendorId = getCurrentVendorId();
        return ResponseEntity.ok(reviewService.getReviewsByVendor(vendorId, pageable));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsForProduct(@PathVariable Long productId, Pageable pageable) {
        Long vendorId = getCurrentVendorId();
        return ResponseEntity.ok(reviewService.getReviewsByVendorAndProduct(vendorId, productId, pageable));
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ReviewSummaryResponse> getProductReviewSummary(@PathVariable Long productId) {
        Long vendorId = getCurrentVendorId();
        return ResponseEntity.ok(reviewService.getVendorProductReviewSummary(vendorId, productId));
    }

    private Long getCurrentVendorId() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedActionException("Authentication required");
        }
        if (!securityUtils.hasRole("ROLE_VENDOR")) {
            throw new UnauthorizedActionException("Only vendors can access vendor reviews");
        }
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found for user"));
        return vendor.getId();
    }
}
