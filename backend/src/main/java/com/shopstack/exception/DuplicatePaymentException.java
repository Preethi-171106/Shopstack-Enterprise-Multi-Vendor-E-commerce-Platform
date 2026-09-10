package com.shopstack.exception;

/**
 * Exception thrown when an order already has an active payment.
 */
public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String message) {
        super(message);
    }
}
