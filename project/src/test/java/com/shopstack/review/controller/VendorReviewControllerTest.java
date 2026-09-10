package com.shopstack.review.controller;

import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.common.exception.UnauthorizedActionException;
import com.shopstack.review.dto.ReviewResponse;
import com.shopstack.review.service.ReviewService;
import com.shopstack.security.JwtAuthEntryPoint;
import com.shopstack.security.JwtAuthenticationFilter;
import com.shopstack.security.JwtTokenProvider;
import com.shopstack.security.SecurityUtils;
import com.shopstack.vendor.entity.Vendor;
import com.shopstack.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendorReviewController.class)
@Import({com.shopstack.security.SecurityConfig.class, JwtAuthEntryPoint.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@AutoConfigureMockMvc
class VendorReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ReviewService reviewService;
    @MockBean private SecurityUtils securityUtils;
    @MockBean private VendorRepository vendorRepository;

    private Vendor vendor;
    private ReviewResponse response;
    private com.shopstack.review.dto.ReviewSummaryResponse summary;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder().id(1L).storeName("TechStore").active(true).build();
        response = new ReviewResponse(
                1L, 1L, "John Doe", 10L, "Laptop", 100L, 5,
                "Great!", "Excellent", "APPROVED",
                Instant.now(), null);
        summary = new com.shopstack.review.dto.ReviewSummaryResponse(
                10L, "Laptop", 4.5, 10L, 6L, 3L, 1L, 0L, 0L);

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.hasRole("ROLE_VENDOR")).thenReturn(true);
        when(vendorRepository.findByUserId(1L)).thenReturn(Optional.of(vendor));
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getReviewsForOwnProducts_returns200() throws Exception {
        Page<ReviewResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(reviewService.getReviewsByVendor(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/vendor/reviews")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getReviewsForProduct_returns200() throws Exception {
        Page<ReviewResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(reviewService.getReviewsByVendorAndProduct(eq(1L), eq(10L), any())).thenReturn(page);

        mockMvc.perform(get("/api/vendor/reviews/product/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getProductReviewSummary_returns200() throws Exception {
        when(reviewService.getVendorProductReviewSummary(1L, 10L)).thenReturn(summary);

        mockMvc.perform(get("/api/vendor/reviews/product/10/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5));
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getProductReviewSummary_notOwnProduct_returns403() throws Exception {
        when(reviewService.getVendorProductReviewSummary(1L, 10L))
                .thenThrow(new UnauthorizedActionException("You can only view reviews for your own products"));

        mockMvc.perform(get("/api/vendor/reviews/product/10/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getReviews_vendorProfileNotFound_returns404() throws Exception {
        when(vendorRepository.findByUserId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/vendor/reviews"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void vendorEndpoint_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/vendor/reviews"))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendorEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/vendor/reviews"))
                .andExpect(status().isUnauthorized());
    }
}
