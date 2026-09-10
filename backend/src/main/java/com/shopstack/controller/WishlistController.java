package com.shopstack.controller;

import com.shopstack.dto.wishlist.WishlistItemResponse;
import com.shopstack.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WishlistController — REST Controller handling all wishlist endpoints.
 *
 * <p>Access: Authenticated users only (enforced in SecurityConfig).
 */
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /**
     * Returns all saved wishlist items for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlist() {
        List<WishlistItemResponse> wishlist = wishlistService.getWishlistForUser();
        return ResponseEntity.ok(wishlist);
    }

    /**
     * Adds a product to the user's wishlist.
     */
    @PostMapping("/items/{productId}")
    public ResponseEntity<WishlistItemResponse> addToWishlist(@PathVariable Long productId) {
        WishlistItemResponse response = wishlistService.addToWishlist(productId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Removes a product from the user's wishlist.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clears all wishlist items for the user.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearWishlist() {
        wishlistService.clearWishlist();
        return ResponseEntity.noContent().build();
    }
}
