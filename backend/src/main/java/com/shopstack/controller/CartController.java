package com.shopstack.controller;

import com.shopstack.dto.cart.CartItemRequest;
import com.shopstack.dto.cart.CartItemUpdateRequest;
import com.shopstack.dto.cart.CartResponse;
import com.shopstack.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CartController — REST Controller handling all shopping cart endpoints.
 *
 * <p>Access: Authenticated users only (enforced in SecurityConfig).
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Returns the shopping cart for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        CartResponse cart = cartService.getCartForUser();
        return ResponseEntity.ok(cart);
    }

    /**
     * Adds an item to the shopping cart.
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody CartItemRequest request
    ) {
        CartResponse cart = cartService.addToCart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    /**
     * Updates the quantity of a cart item.
     */
    @PutMapping("/items/{id}")
    public ResponseEntity<CartResponse> updateCartItemQuantity(
            @PathVariable Long id,
            @Valid @RequestBody CartItemUpdateRequest request
    ) {
        CartResponse cart = cartService.updateCartItemQuantity(id, request);
        return ResponseEntity.ok(cart);
    }

    /**
     * Removes a single cart item.
     */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> removeCartItem(@PathVariable Long id) {
        cartService.removeCartItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clears all items in the user's shopping cart.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }
}
