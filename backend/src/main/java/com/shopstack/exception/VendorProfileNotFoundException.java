package com.shopstack.exception;

/**
 * VendorProfileNotFoundException — Thrown when a vendor profile is not found for the user.
 */
public class VendorProfileNotFoundException extends RuntimeException {

    public VendorProfileNotFoundException(String message) {
        super(message);
    }
}
