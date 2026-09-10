package com.shopstack.review.service;

import com.shopstack.common.entity.User;
import com.shopstack.common.exception.BadRequestException;
import com.shopstack.common.exception.ConflictException;
import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.common.exception.UnauthorizedActionException;
import com.shopstack.common.repository.UserRepository;
import com.shopstack.order.entity.Order;
import com.shopstack.order.entity.OrderItem;
import com.shopstack.order.enums.OrderStatus;
import com.shopstack.order.repository.OrderRepository;
import com.shopstack.product.entity.Product;
import com.shopstack.product.enums.ProductStatus;
import com.shopstack.product.repository.ProductRepository;
import com.shopstack.review.dto.CreateReviewRequest;
import com.shopstack.review.dto.ReviewResponse;
import com.shopstack.review.dto.ReviewSummaryResponse;
import com.shopstack.review.dto.UpdateReviewRequest;
import com.shopstack.review.entity.Review;
import com.shopstack.review.enums.ReviewStatus;
import com.shopstack.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ReviewService reviewService;

    private User user;
    private Product product;
    private Order order;
    private OrderItem orderItem;
    private Review review;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).firstName("John").lastName("Doe").email("john@test.com").build();
        product = Product.builder()
                .id(10L).name("Laptop").price(BigDecimal.valueOf(999))
                .status(ProductStatus.ACTIVE).averageRating(0.0).totalReviews(0L)
                .build();
        order = Order.builder()
                .id(100L).orderNumber("ORD-100").customer(user)
                .status(OrderStatus.DELIVERED).totalAmount(BigDecimal.valueOf(999))
                .build();
        orderItem = OrderItem.builder()
                .id(1L).order(order).product(product).quantity(1)
                .unitPrice(BigDecimal.valueOf(999)).subtotal(BigDecimal.valueOf(999))
                .build();
        order.getItems().add(orderItem);

        review = Review.builder()
                .id(1L).user(user).product(product).order(order)
                .rating(5).title("Great!").comment("Excellent product")
                .status(ReviewStatus.PENDING)
                .build();
    }

    // ===== Create Review =====

    @Test
    void createReview_validInput_returnsReview() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 5, "Great!", "Excellent product");
        ReviewResponse result = reviewService.createReview(1L, request);

        assertNotNull(result);
        assertEquals(5, result.rating());
        assertEquals("Great!", result.title());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_userNotFound_throwsNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 5, "Great!", "Excellent");
        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(999L, request));
    }

    @Test
    void createReview_productNotFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        CreateReviewRequest request = new CreateReviewRequest(999L, 100L, 5, "Great!", "Excellent");
        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(1L, request));
    }

    @Test
    void createReview_orderNotFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        CreateReviewRequest request = new CreateReviewRequest(10L, 999L, 5, "Great!", "Excellent");
        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(1L, request));
    }

    @Test
    void createReview_orderNotOwned_throwsUnauthorized() {
        User otherUser = User.builder().id(2L).build();
        order.setCustomer(otherUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 5, "Great!", "Excellent");
        assertThrows(UnauthorizedActionException.class,
                () -> reviewService.createReview(1L, request));
    }

    @Test
    void createReview_cancelledOrder_throwsBadRequest() {
        order.setStatus(OrderStatus.CANCELLED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 5, "Great!", "Excellent");
        assertThrows(BadRequestException.class,
                () -> reviewService.createReview(1L, request));
    }

    @Test
    void createReview_productNotInOrder_throwsBadRequest() {
        Product otherProduct = Product.builder().id(99L).name("Tablet").build();
        orderItem.setProduct(otherProduct);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 5, "Great!", "Excellent");
        assertThrows(BadRequestException.class,
                () -> reviewService.createReview(1L, request));
    }

    @Test
    void createReview_duplicateReview_throwsConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true);

        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 5, "Great!", "Excellent");
        assertThrows(ConflictException.class,
                () -> reviewService.createReview(1L, request));
    }

    // ===== Update Review =====

    @Test
    void updateReview_ownReview_returnsUpdated() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated", "Updated comment");
        ReviewResponse result = reviewService.updateReview(1L, 1L, request);

        assertNotNull(result);
        assertEquals(4, review.getRating());
        assertEquals("Updated", review.getTitle());
    }

    @Test
    void updateReview_notOwner_throwsUnauthorized() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated", "Updated comment");
        assertThrows(UnauthorizedActionException.class,
                () -> reviewService.updateReview(999L, 1L, request));
    }

    @Test
    void updateReview_notFound_throwsNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated", "Updated comment");
        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.updateReview(1L, 999L, request));
    }

    // ===== Delete Review =====

    @Test
    void deleteReview_ownReview_deletesAndRecalculates() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.calculateAverageAndCountForProduct(10L))
                .thenReturn(new Object[]{0.0, 0L});
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        reviewService.deleteReview(1L, 1L);

        verify(reviewRepository).delete(review);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void deleteReview_notOwner_throwsUnauthorized() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThrows(UnauthorizedActionException.class,
                () -> reviewService.deleteReview(999L, 1L));
    }

    @Test
    void deleteReview_notFound_throwsNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.deleteReview(1L, 999L));
    }

    // ===== Get My Reviews =====

    @Test
    void getMyReviews_returnsPage() {
        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any())).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getMyReviews(1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    // ===== Get Product Reviews =====

    @Test
    void getProductReviews_approvedOnly_returnsPage() {
        review.setStatus(ReviewStatus.APPROVED);
        Page<Review> page = new PageImpl<>(List.of(review));
        when(productRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(eq(10L), eq(ReviewStatus.APPROVED), any()))
                .thenReturn(page);

        Page<ReviewResponse> result = reviewService.getProductReviews(10L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProductReviews_productNotFound_throwsNotFound() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getProductReviews(999L, PageRequest.of(0, 10)));
    }

    // ===== Get Product Review Summary =====

    @Test
    void getProductReviewSummary_returnsSummary() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(reviewRepository.calculateAverageAndCountForProduct(10L))
                .thenReturn(new Object[]{4.5, 10L});
        when(reviewRepository.countByRatingGroupForProduct(10L))
                .thenReturn(List.of(
                        new Object[]{5, 6L},
                        new Object[]{4, 3L},
                        new Object[]{3, 1L}
                ));

        ReviewSummaryResponse result = reviewService.getProductReviewSummary(10L);

        assertEquals(10L, product.getId());
        assertEquals(4.5, result.averageRating());
        assertEquals(10L, result.totalReviews());
        assertEquals(6L, result.fiveStarCount());
        assertEquals(3L, result.fourStarCount());
        assertEquals(1L, result.threeStarCount());
        assertEquals(0L, result.twoStarCount());
        assertEquals(0L, result.oneStarCount());
    }

    @Test
    void getProductReviewSummary_noReviews_returnsZeros() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(reviewRepository.calculateAverageAndCountForProduct(10L))
                .thenReturn(new Object[]{0.0, 0L});
        when(reviewRepository.countByRatingGroupForProduct(10L))
                .thenReturn(List.of());

        ReviewSummaryResponse result = reviewService.getProductReviewSummary(10L);

        assertEquals(0.0, result.averageRating());
        assertEquals(0L, result.totalReviews());
        assertEquals(0L, result.fiveStarCount());
    }

    @Test
    void getProductReviewSummary_productNotFound_throwsNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getProductReviewSummary(999L));
    }

    // ===== Admin: Get All Reviews =====

    @Test
    void getAllReviews_returnsPage() {
        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getAllReviews(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    // ===== Admin: Approve Review =====

    @Test
    void approveReview_validId_updatesStatusAndRecalculates() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewRepository.calculateAverageAndCountForProduct(10L))
                .thenReturn(new Object[]{5.0, 1L});
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ReviewResponse result = reviewService.approveReview(1L);

        assertEquals(ReviewStatus.APPROVED, review.getStatus());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void approveReview_notFound_throwsNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.approveReview(999L));
    }

    // ===== Admin: Reject Review =====

    @Test
    void rejectReview_validId_updatesStatusAndRecalculates() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewRepository.calculateAverageAndCountForProduct(10L))
                .thenReturn(new Object[]{0.0, 0L});
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ReviewResponse result = reviewService.rejectReview(1L);

        assertEquals(ReviewStatus.REJECTED, review.getStatus());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void rejectReview_notFound_throwsNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.rejectReview(999L));
    }

    // ===== Admin: Delete Review =====

    @Test
    void deleteReviewByAdmin_validId_deletesAndRecalculates() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.calculateAverageAndCountForProduct(10L))
                .thenReturn(new Object[]{0.0, 0L});
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        reviewService.deleteReviewByAdmin(1L);

        verify(reviewRepository).delete(review);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void deleteReviewByAdmin_notFound_throwsNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.deleteReviewByAdmin(999L));
    }

    // ===== Vendor: Get Reviews =====

    @Test
    void getReviewsByVendor_returnsPage() {
        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository.findByProductVendorId(eq(1L), any())).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getReviewsByVendor(1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getReviewsByVendorAndProduct_returnsPage() {
        Page<Review> page = new PageImpl<>(List.of(review));
        when(productRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.findByProductIdAndVendorId(eq(10L), eq(1L), any())).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getReviewsByVendorAndProduct(1L, 10L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getReviewsByVendorAndProduct_productNotFound_throwsNotFound() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getReviewsByVendorAndProduct(1L, 999L, PageRequest.of(0, 10)));
    }

    // ===== Vendor: Get Product Review Summary =====

    @Test
    void getVendorProductReviewSummary_ownProduct_returnsSummary() {
        com.shopstack.vendor.entity.Vendor vendor = com.shopstack.vendor.entity.Vendor.builder()
                .id(1L).storeName("TechStore").build();
        product.setVendor(vendor);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(reviewRepository.calculateAverageAndCountForProduct(10L))
                .thenReturn(new Object[]{4.5, 10L});
        when(reviewRepository.countByRatingGroupForProduct(10L))
                .thenReturn(List.of());

        ReviewSummaryResponse result = reviewService.getVendorProductReviewSummary(1L, 10L);

        assertEquals(4.5, result.averageRating());
    }

    @Test
    void getVendorProductReviewSummary_notOwnProduct_throwsUnauthorized() {
        com.shopstack.vendor.entity.Vendor otherVendor = com.shopstack.vendor.entity.Vendor.builder()
                .id(999L).storeName("OtherStore").build();
        product.setVendor(otherVendor);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThrows(UnauthorizedActionException.class,
                () -> reviewService.getVendorProductReviewSummary(1L, 10L));
    }

    @Test
    void getVendorProductReviewSummary_productNotFound_throwsNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getVendorProductReviewSummary(1L, 999L));
    }
}
