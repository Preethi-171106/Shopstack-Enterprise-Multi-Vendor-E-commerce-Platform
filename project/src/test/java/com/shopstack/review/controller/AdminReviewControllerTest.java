package com.shopstack.review.controller;

import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.review.dto.ReviewResponse;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReviewController.class)
@Import({com.shopstack.security.SecurityConfig.class, JwtAuthEntryPoint.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@AutoConfigureMockMvc
class AdminReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ReviewService reviewService;
    @MockBean private SecurityUtils securityUtils;

    private ReviewResponse response;

    @BeforeEach
    void setUp() {
        response = new ReviewResponse(
                1L, 1L, "John Doe", 10L, "Laptop", 100L, 5,
                "Great!", "Excellent", "PENDING",
                Instant.now(), null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllReviews_returns200() throws Exception {
        Page<ReviewResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(reviewService.getAllReviews(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/reviews")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveReview_returns200() throws Exception {
        ReviewResponse approved = new ReviewResponse(
                1L, 1L, "John Doe", 10L, "Laptop", 100L, 5,
                "Great!", "Excellent", "APPROVED",
                Instant.now(), Instant.now());
        when(reviewService.approveReview(1L)).thenReturn(approved);

        mockMvc.perform(patch("/api/admin/reviews/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectReview_returns200() throws Exception {
        ReviewResponse rejected = new ReviewResponse(
                1L, 1L, "John Doe", 10L, "Laptop", 100L, 5,
                "Great!", "Excellent", "REJECTED",
                Instant.now(), Instant.now());
        when(reviewService.rejectReview(1L)).thenReturn(rejected);

        mockMvc.perform(patch("/api/admin/reviews/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReview_returns204() throws Exception {
        doNothing().when(reviewService).deleteReviewByAdmin(1L);

        mockMvc.perform(delete("/api/admin/reviews/1"))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReviewByAdmin(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveReview_notFound_returns404() throws Exception {
        when(reviewService.approveReview(999L))
                .thenThrow(new ResourceNotFoundException("Review", 999L));

        mockMvc.perform(patch("/api/admin/reviews/999/approve"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectReview_notFound_returns404() throws Exception {
        when(reviewService.rejectReview(999L))
                .thenThrow(new ResourceNotFoundException("Review", 999L));

        mockMvc.perform(patch("/api/admin/reviews/999/reject"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReview_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Review", 999L))
                .when(reviewService).deleteReviewByAdmin(999L);

        mockMvc.perform(delete("/api/admin/reviews/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminEndpoint_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/reviews"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/reviews"))
                .andExpect(status().isUnauthorized());
    }
}
