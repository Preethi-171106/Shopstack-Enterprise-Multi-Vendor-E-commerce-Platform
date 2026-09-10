package com.shopstack.review.service;

import com.shopstack.common.entity.User;
import com.shopstack.common.exception.BadRequestException;
import com.shopstack.common.exception.ConflictException;
import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.common.exception.UnauthorizedActionException;
import com.shopstack.order.entity.Order;
import com.shopstack.order.enums.OrderStatus;
import com.shopstack.order.repository.OrderRepository;
import com.shopstack.product.entity.Product;
import com.shopstack.product.repository.ProductRepository;
import com.shopstack.review.dto.CreateReviewRequest;
import com.shopstack.review.dto.ReviewResponse;
import com.shopstack.review.dto.ReviewSummaryResponse;
import com.shopstack.review.dto.UpdateReviewRequest;
import com.shopstack.review.entity.Review;
import com.shopstack.review.enums.ReviewStatus;
import com.shopstack.review.mapper.ReviewMapper;
import com.shopstack.review.repository.ReviewRepository;
import com.shopstack.common.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         ProductRepository productRepository,
                         OrderRepository orderRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // ===== Customer: Create Review =====

    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.orderId()));

        // Validate order belongs to user
        if (!order.getCustomer().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only review products from your own orders");
        }

        // Validate order is not cancelled
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot review products from cancelled orders");
        }

        // Validate product was in the order
        boolean productInOrder = order.getItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(request.productId()));
        if (!productInOrder) {
            throw new BadRequestException("The product was not part of this order");
        }

        // Check for duplicate review
        if (reviewRepository.existsByUserIdAndProductId(userId, request.productId())) {
            throw new ConflictException("You have already reviewed this product");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .order(order)
                .rating(request.rating())
                .title(request.title())
                .comment(request.comment())
                .status(ReviewStatus.PENDING)
                .build();

        Review saved = reviewRepository.save(review);
        return ReviewMapper.toResponse(saved);
    }

    // ===== Customer: Update Own Review =====

    public ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only edit your own reviews");
        }

        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setComment(request.comment());
        review.setStatus(ReviewStatus.PENDING);

        Review saved = reviewRepository.save(review);
        return ReviewMapper.toResponse(saved);
    }

    // ===== Customer: Delete Own Review =====

    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only delete your own reviews");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        recalculateProductRating(productId);
    }

    // ===== Customer: View Own Reviews =====

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(ReviewMapper::toResponse);
    }

    // ===== Public: View Product Reviews (only approved) =====

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED, pageable)
                .map(ReviewMapper::toResponse);
    }

    // ===== Public: View Product Rating Summary =====

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getProductReviewSummary(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        Object[] avgCount = reviewRepository.calculateAverageAndCountForProduct(productId);
        Double averageRating = avgCount[0] != null ? ((Double) avgCount[0]) : 0.0;
        Long totalReviews = avgCount[1] != null ? ((Long) avgCount[1]) : 0L;

        List<Object[]> distribution = reviewRepository.countByRatingGroupForProduct(productId);
        long fiveStar = 0, fourStar = 0, threeStar = 0, twoStar = 0, oneStar = 0;
        for (Object[] row : distribution) {
            int rating = (Integer) row[0];
            long count = (Long) row[1];
            switch (rating) {
                case 5 -> fiveStar = count;
                case 4 -> fourStar = count;
                case 3 -> threeStar = count;
                case 2 -> twoStar = count;
                case 1 -> oneStar = count;
            }
        }

        return new ReviewSummaryResponse(
                productId,
                product.getName(),
                Math.round(averageRating * 100.0) / 100.0,
                totalReviews,
                fiveStar, fourStar, threeStar, twoStar, oneStar
        );
    }

    // ===== Admin: View All Reviews =====

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviews(Pageable pageable) {
        return reviewRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ReviewMapper::toResponse);
    }

    // ===== Admin: Approve Review =====

    public ReviewResponse approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        review.setStatus(ReviewStatus.APPROVED);
        Review saved = reviewRepository.save(review);
        recalculateProductRating(review.getProduct().getId());
        return ReviewMapper.toResponse(saved);
    }

    // ===== Admin: Reject Review =====

    public ReviewResponse rejectReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        review.setStatus(ReviewStatus.REJECTED);
        Review saved = reviewRepository.save(review);
        recalculateProductRating(review.getProduct().getId());
        return ReviewMapper.toResponse(saved);
    }

    // ===== Admin: Delete Review =====

    public void deleteReviewByAdmin(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        recalculateProductRating(productId);
    }

    // ===== Vendor: View Reviews for Own Products =====

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByVendor(Long vendorId, Pageable pageable) {
        return reviewRepository.findByProductVendorId(vendorId, pageable)
                .map(ReviewMapper::toResponse);
    }

    // ===== Vendor: View Reviews for Specific Product =====

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByVendorAndProduct(Long vendorId, Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return reviewRepository.findByProductIdAndVendorId(productId, vendorId, pageable)
                .map(ReviewMapper::toResponse);
    }

    // ===== Vendor: View Average Rating of Own Products =====

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getVendorProductReviewSummary(Long vendorId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getVendor() == null || !product.getVendor().getId().equals(vendorId)) {
            throw new UnauthorizedActionException("You can only view reviews for your own products");
        }

        return getProductReviewSummary(productId);
    }

    // ===== Rating Recalculation =====

    private void recalculateProductRating(Long productId) {
        Object[] avgCount = reviewRepository.calculateAverageAndCountForProduct(productId);
        Double averageRating = avgCount[0] != null ? ((Double) avgCount[0]) : 0.0;
        Long totalReviews = avgCount[1] != null ? ((Long) avgCount[1]) : 0L;

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        product.setAverageRating(Math.round(averageRating * 100.0) / 100.0);
        product.setTotalReviews(totalReviews);
        productRepository.save(product);
    }
}
