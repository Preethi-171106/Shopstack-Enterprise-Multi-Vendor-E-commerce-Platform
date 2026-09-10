package com.shopstack.exception;

/**
 * InvalidCredentialsException — Thrown when login fails due to incorrect email or password.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
