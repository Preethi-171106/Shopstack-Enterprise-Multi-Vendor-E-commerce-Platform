package com.shopstack.exception;

/**
 * UserDisabledException — Thrown when login is attempted on a disabled user account.
 */
public class UserDisabledException extends RuntimeException {

    public UserDisabledException(String message) {
        super(message);
    }
}
