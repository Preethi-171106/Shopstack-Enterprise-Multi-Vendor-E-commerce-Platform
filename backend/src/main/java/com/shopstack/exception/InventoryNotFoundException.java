package com.shopstack.exception;

/**
 * InventoryNotFoundException — Thrown when an inventory record is not found for a product (HTTP 404).
 */
public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String message) {
        super(message);
    }
}
