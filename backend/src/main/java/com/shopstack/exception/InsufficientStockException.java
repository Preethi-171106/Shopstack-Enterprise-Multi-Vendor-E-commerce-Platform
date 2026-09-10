package com.shopstack.exception;

/**
 * Exception thrown when requested cart quantity exceeds available product stock.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
