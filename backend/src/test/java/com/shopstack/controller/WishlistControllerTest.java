package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.wishlist.WishlistItemResponse;
import com.shopstack.exception.ProductNotFoundException;
import com.shopstack.exception.WishlistItemNotFoundException;
import com.shopstack.service.WishlistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WishlistService wishlistService;

    private WishlistItemResponse sampleWishlistItemResponse() {
        return new WishlistItemResponse(
                5L,
                1L,
                "iPhone 15",
                "iphone-15",
                new BigDecimal("999.99"),
                "https://example.com/iphone.jpg",
                1L,
                "TechStore",
                new BigDecimal("4.8"),
                120,
                50,
                true,
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /api/wishlist — Get User Wishlist")
    class GetWishlist {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Authenticated user — returns wishlist items (200 OK)")
        void getWishlist_Success() throws Exception {
            given(wishlistService.getWishlistForUser()).willReturn(List.of(sampleWishlistItemResponse()));

            mockMvc.perform(get("/api/wishlist"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].productName").value("iPhone 15"));
        }

        @Test
        @DisplayName("Unauthenticated user — returns 401 Unauthorized")
        void getWishlist_Unauthenticated() throws Exception {
            mockMvc.perform(get("/api/wishlist"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/wishlist/items/{productId} — Add Product to Wishlist")
    class AddToWishlist {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Authenticated user — adds product to wishlist (201 Created)")
        void addToWishlist_Success() throws Exception {
            given(wishlistService.addToWishlist(1L)).willReturn(sampleWishlistItemResponse());

            mockMvc.perform(post("/api/wishlist/items/1"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productId").value(1L))
                    .andExpect(jsonPath("$.productName").value("iPhone 15"));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Non-existent product — returns 404 Not Found")
        void addToWishlist_ProductNotFound() throws Exception {
            given(wishlistService.addToWishlist(999L))
                    .willThrow(new ProductNotFoundException("Product with ID 999 not found or is inactive"));

            mockMvc.perform(post("/api/wishlist/items/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("DELETE /api/wishlist/items/{productId} — Remove Product from Wishlist")
    class RemoveFromWishlist {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Authenticated user — removes product from wishlist (204 No Content)")
        void removeFromWishlist_Success() throws Exception {
            willDoNothing().given(wishlistService).removeFromWishlist(1L);

            mockMvc.perform(delete("/api/wishlist/items/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Product not in wishlist — returns 404 Not Found")
        void removeFromWishlist_NotFound() throws Exception {
            willThrow(new WishlistItemNotFoundException("Product with ID 999 is not in user's wishlist"))
                    .given(wishlistService).removeFromWishlist(999L);

            mockMvc.perform(delete("/api/wishlist/items/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("DELETE /api/wishlist — Clear User Wishlist")
    class ClearWishlist {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Authenticated user — clears wishlist (204 No Content)")
        void clearWishlist_Success() throws Exception {
            willDoNothing().given(wishlistService).clearWishlist();

            mockMvc.perform(delete("/api/wishlist"))
                    .andExpect(status().isNoContent());
        }
    }
}
