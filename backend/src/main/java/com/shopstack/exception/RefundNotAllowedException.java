package com.shopstack.exception;

/**
 * Exception thrown when a refund cannot be performed on the payment (e.g., not in SUCCESS status).
 */
public class RefundNotAllowedException extends RuntimeException {
    public RefundNotAllowedException(String message) {
        super(message);
    }
}
