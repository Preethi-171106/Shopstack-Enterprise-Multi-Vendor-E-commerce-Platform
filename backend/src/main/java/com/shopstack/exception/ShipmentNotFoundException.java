package com.shopstack.exception;

/**
 * Exception thrown when a requested shipment is not found.
 */
public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(String message) {
        super(message);
    }
}
