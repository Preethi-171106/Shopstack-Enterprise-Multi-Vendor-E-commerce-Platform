package com.shopstack.exception;

public class CouponMinimumAmountException extends RuntimeException {
    public CouponMinimumAmountException(String message) {
        super(message);
    }
}
