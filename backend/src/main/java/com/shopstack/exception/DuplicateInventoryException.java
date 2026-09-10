package com.shopstack.exception;

/**
 * DuplicateInventoryException — Thrown when attempting to create an inventory record for a product that already has one (HTTP 409).
 */
public class DuplicateInventoryException extends RuntimeException {
    public DuplicateInventoryException(String message) {
        super(message);
    }
}
