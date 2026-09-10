package com.shopstack.exception;

/**
 * RazorpayNotConfiguredException — thrown when Razorpay credentials are not provided
 * but a payment operation requiring the gateway is attempted.
 */
public class RazorpayNotConfiguredException extends RuntimeException {
    public RazorpayNotConfiguredException(String message) {
        super(message);
    }
}
