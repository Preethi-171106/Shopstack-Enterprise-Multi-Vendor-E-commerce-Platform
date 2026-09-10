package com.shopstack.exception;

/**
 * InvalidStockOperationException — Thrown when a requested stock operation violates domain rules (HTTP 400).
 * E.g., negative quantity inputs, releasing more stock than reserved, adjusting to negative numbers.
 */
public class InvalidStockOperationException extends RuntimeException {
    public InvalidStockOperationException(String message) {
        super(message);
    }
}
