package com.shopstack.exception;

public class CouponInactiveException extends RuntimeException {
    public CouponInactiveException(String message) {
        super(message);
    }
}
