package com.shopstack.exception;

public class CouponUsageExceededException extends RuntimeException {
    public CouponUsageExceededException(String message) {
        super(message);
    }
}
