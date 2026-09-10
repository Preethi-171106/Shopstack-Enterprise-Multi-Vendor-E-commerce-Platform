package com.shopstack.exception;

/**
 * Exception thrown when a requested order is not found.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
