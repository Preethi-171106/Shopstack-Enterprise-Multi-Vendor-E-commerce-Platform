package com.shopstack.exception;

/**
 * Exception thrown when a requested wishlist item is not found or does not belong to the user.
 */
public class WishlistItemNotFoundException extends RuntimeException {
    public WishlistItemNotFoundException(String message) {
        super(message);
    }
}
