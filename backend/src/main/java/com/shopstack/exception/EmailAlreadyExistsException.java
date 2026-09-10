package com.shopstack.exception;

/**
 * EmailAlreadyExistsException — Thrown when registration is attempted with an email already present in the database.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
