package com.shopstack.exception;

/**
 * VendorProfileAlreadyExistsException — Thrown when a vendor attempts to create a second profile.
 */
public class VendorProfileAlreadyExistsException extends RuntimeException {

    public VendorProfileAlreadyExistsException(String message) {
        super(message);
    }
}
