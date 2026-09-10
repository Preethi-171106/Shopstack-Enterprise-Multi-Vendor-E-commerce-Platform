package com.shopstack.exception;

/**
 * Exception thrown when Razorpay payment signature verification fails.
 */
public class PaymentVerificationException extends RuntimeException {
    public PaymentVerificationException(String message) {
        super(message);
    }
}
