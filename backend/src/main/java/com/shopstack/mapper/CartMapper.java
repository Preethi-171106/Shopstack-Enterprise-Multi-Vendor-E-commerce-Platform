package com.shopstack.mapper;

import com.shopstack.dto.cart.CartItemResponse;
import com.shopstack.dto.cart.CartResponse;
import com.shopstack.entity.CartItem;
import com.shopstack.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * CartMapper — Maps {@link CartItem} entities to {@link CartItemResponse} and builds {@link CartResponse}.
 */
@Component
public class CartMapper {

    public CartItemResponse toItemResponse(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }

        Product product = cartItem.getProduct();
        BigDecimal price = product.getPrice();
        int quantity = cartItem.getQuantity();
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

        return new CartItemResponse(
                cartItem.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                price,
                product.getImageUrl(),
                product.getVendorProfile().getId(),
                product.getVendorProfile().getStoreName(),
                quantity,
                subtotal,
                product.isActive(),
                product.getStockQuantity()
        );
    }

    public CartResponse toCartResponse(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return new CartResponse(List.of(), BigDecimal.ZERO, 0, 0);
        }

        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal grandSubtotal = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.size();
        int totalQuantity = itemResponses.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        return new CartResponse(itemResponses, grandSubtotal, totalItems, totalQuantity);
    }
}
