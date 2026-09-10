package com.shopstack.exception;

public class DuplicateReturnException extends RuntimeException {
    public DuplicateReturnException(String message) {
        super(message);
    }
}
