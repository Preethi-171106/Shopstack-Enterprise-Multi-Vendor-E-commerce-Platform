package com.shopstack.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.common.exception.BadRequestException;
import com.shopstack.common.exception.ConflictException;
import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.common.exception.UnauthorizedActionException;
import com.shopstack.review.dto.CreateReviewRequest;
import com.shopstack.review.dto.ReviewResponse;
import com.shopstack.review.dto.ReviewSummaryResponse;
import com.shopstack.review.dto.UpdateReviewRequest;
import com.shopstack.review.service.ReviewService;
import com.shopstack.security.JwtAuthEntryPoint;
import com.shopstack.security.JwtAuthenticationFilter;
import com.shopstack.security.JwtTokenProvider;
import com.shopstack.security.SecurityUtils;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import({com.shopstack.security.SecurityConfig.class, JwtAuthEntryPoint.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@AutoConfigureMockMvc
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ReviewService reviewService;
    @MockBean private SecurityUtils securityUtils;

    private ReviewResponse response;
    private ReviewSummaryResponse summary;

    @BeforeEach
    void setUp() {
        response = new ReviewResponse(
                1L, 1L, "John Doe", 10L, "Laptop", 100L, 5,
                "Great!", "Excellent product", "PENDING",
                Instant.now(), null);
        summary = new ReviewSummaryResponse(10L, "Laptop", 4.5, 10L,
                6L, 3L, 1L, 0L, 0L);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createReview_validInput_returns201() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 5, "Great!", "Excellent product");
        when(reviewService.createReview(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.title").value("Great!"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createReview_duplicateReview_returns409() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 5, "Great!", "Excellent");
        when(reviewService.createReview(eq(1L), any()))
                .thenThrow(new ConflictException("You have already reviewed this product"));

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createReview_invalidRating_returns400() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 0, "Great!", "Excellent");

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createReview_missingFields_returns400() throws Exception {
        String json = "{}";

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateReview_ownReview_returns200() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated", "Updated comment");
        ReviewResponse updated = new ReviewResponse(
                1L, 1L, "John Doe", 10L, "Laptop", 100L, 4,
                "Updated", "Updated comment", "PENDING",
                Instant.now(), Instant.now());
        when(reviewService.updateReview(eq(1L), eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateReview_notOwner_returns403() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated", "Updated comment");
        when(reviewService.updateReview(eq(1L), eq(1L), any()))
                .thenThrow(new UnauthorizedActionException("You can only edit your own reviews"));

        mockMvc.perform(put("/api/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateReview_notFound_returns404() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated", "Updated comment");
        when(reviewService.updateReview(eq(1L), eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Review", 999L));

        mockMvc.perform(put("/api/reviews/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteReview_ownReview_returns204() throws Exception {
        doNothing().when(reviewService).deleteReview(1L, 1L);

        mockMvc.perform(delete("/api/reviews/1"))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(1L, 1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteReview_notOwner_returns403() throws Exception {
        doThrow(new UnauthorizedActionException("You can only delete your own reviews"))
                .when(reviewService).deleteReview(1L, 1L);

        mockMvc.perform(delete("/api/reviews/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteReview_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Review", 999L))
                .when(reviewService).deleteReview(1L, 999L);

        mockMvc.perform(delete("/api/reviews/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getMyReviews_returns200() throws Exception {
        Page<ReviewResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(reviewService.getMyReviews(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/reviews/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getProductReviews_publicAccess_returns200() throws Exception {
        Page<ReviewResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(reviewService.getProductReviews(eq(10L), any())).thenReturn(page);

        mockMvc.perform(get("/api/reviews/product/10")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getProductReviewSummary_publicAccess_returns200() throws Exception {
        when(reviewService.getProductReviewSummary(10L)).thenReturn(summary);

        mockMvc.perform(get("/api/reviews/product/10/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.totalReviews").value(10))
                .andExpect(jsonPath("$.fiveStarCount").value(6));
    }

    @Test
    void getProductReviewSummary_productNotFound_returns404() throws Exception {
        when(reviewService.getProductReviewSummary(999L))
                .thenThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(get("/api/reviews/product/999/summary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductReviews_productNotFound_returns404() throws Exception {
        when(reviewService.getProductReviews(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(get("/api/reviews/product/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createReview_cancelledOrder_returns400() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 5, "Great!", "Excellent");
        when(reviewService.createReview(eq(1L), any()))
                .thenThrow(new BadRequestException("Cannot review products from cancelled orders"));

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReview_unauthenticated_returns401() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(10L, 100L, 5, "Great!", "Excellent");

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
