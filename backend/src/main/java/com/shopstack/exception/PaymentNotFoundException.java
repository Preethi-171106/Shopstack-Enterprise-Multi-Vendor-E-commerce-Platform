package com.shopstack.exception;

/**
 * Exception thrown when a requested payment record is not found.
 */
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
