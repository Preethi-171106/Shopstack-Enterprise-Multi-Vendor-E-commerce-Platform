package com.shopstack.exception;

/**
 * Exception thrown when a requested cart item is not found or does not belong to the user.
 */
public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(String message) {
        super(message);
    }
}
