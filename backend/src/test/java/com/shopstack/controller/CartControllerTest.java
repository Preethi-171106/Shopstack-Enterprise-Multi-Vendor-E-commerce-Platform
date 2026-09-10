package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.cart.CartItemRequest;
import com.shopstack.dto.cart.CartItemResponse;
import com.shopstack.dto.cart.CartItemUpdateRequest;
import com.shopstack.dto.cart.CartResponse;
import com.shopstack.exception.CartItemNotFoundException;
import com.shopstack.exception.InsufficientStockException;
import com.shopstack.exception.ProductNotFoundException;
import com.shopstack.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    private CartItemResponse sampleCartItemResponse() {
        return new CartItemResponse(
                10L,
                1L,
                "iPhone 15",
                "iphone-15",
                new BigDecimal("999.99"),
                "https://example.com/iphone.jpg",
                1L,
                "TechStore",
                2,
                new BigDecimal("1999.98"),
                true,
                50
        );
    }

    private CartResponse sampleCartResponse() {
        return new CartResponse(
                List.of(sampleCartItemResponse()),
                new BigDecimal("1999.98"),
                1,
                2
        );
    }

    @Nested
    @DisplayName("GET /api/cart — Get User Shopping Cart")
    class GetCart {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Authenticated user — returns cart (200 OK)")
        void getCart_Success() throws Exception {
            given(cartService.getCartForUser()).willReturn(sampleCartResponse());

            mockMvc.perform(get("/api/cart"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.subtotal").value(1999.98))
                    .andExpect(jsonPath("$.totalQuantity").value(2));
        }

        @Test
        @DisplayName("Unauthenticated user — returns 401 Unauthorized")
        void getCart_Unauthenticated() throws Exception {
            mockMvc.perform(get("/api/cart"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/cart/items — Add Item to Cart")
    class AddToCart {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Authenticated user — adds item to cart (201 Created)")
        void addToCart_Success() throws Exception {
            CartItemRequest request = new CartItemRequest(1L, 2);
            given(cartService.addToCart(any(CartItemRequest.class))).willReturn(sampleCartResponse());

            mockMvc.perform(post("/api/cart/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.items[0].productId").value(1L))
                    .andExpect(jsonPath("$.items[0].quantity").value(2));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Insufficient stock — returns 400 Bad Request")
        void addToCart_InsufficientStock() throws Exception {
            CartItemRequest request = new CartItemRequest(1L, 100);
            given(cartService.addToCart(any(CartItemRequest.class)))
                    .willThrow(new InsufficientStockException("Insufficient stock for product 'iPhone 15'. Available: 50, requested total: 100"));

            mockMvc.perform(post("/api/cart/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Insufficient stock for product 'iPhone 15'. Available: 50, requested total: 100"));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Non-existent product — returns 404 Not Found")
        void addToCart_ProductNotFound() throws Exception {
            CartItemRequest request = new CartItemRequest(999L, 1);
            given(cartService.addToCart(any(CartItemRequest.class)))
                    .willThrow(new ProductNotFoundException("Product with ID 999 not found or is inactive"));

            mockMvc.perform(post("/api/cart/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("PUT /api/cart/items/{id} — Update Cart Item Quantity")
    class UpdateCartItem {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Authenticated user — updates quantity (200 OK)")
        void updateQuantity_Success() throws Exception {
            CartItemUpdateRequest request = new CartItemUpdateRequest(5);
            given(cartService.updateCartItemQuantity(eq(10L), any(CartItemUpdateRequest.class)))
                    .willReturn(sampleCartResponse());

            mockMvc.perform(put("/api/cart/items/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Item not found — returns 404 Not Found")
        void updateQuantity_NotFound() throws Exception {
            CartItemUpdateRequest request = new CartItemUpdateRequest(5);
            given(cartService.updateCartItemQuantity(eq(999L), any(CartItemUpdateRequest.class)))
                    .willThrow(new CartItemNotFoundException("Cart item with ID 999 not found in user's cart"));

            mockMvc.perform(put("/api/cart/items/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/cart/items/{id} — Remove Cart Item")
    class RemoveCartItem {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Authenticated user — removes cart item (204 No Content)")
        void removeItem_Success() throws Exception {
            willDoNothing().given(cartService).removeCartItem(10L);

            mockMvc.perform(delete("/api/cart/items/10"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("DELETE /api/cart — Clear Shopping Cart")
    class ClearCart {

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("Authenticated user — clears cart (204 No Content)")
        void clearCart_Success() throws Exception {
            willDoNothing().given(cartService).clearCart();

            mockMvc.perform(delete("/api/cart"))
                    .andExpect(status().isNoContent());
        }
    }
}
