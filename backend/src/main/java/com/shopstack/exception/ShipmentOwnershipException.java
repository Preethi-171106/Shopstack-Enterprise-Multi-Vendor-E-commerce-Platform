package com.shopstack.exception;

/**
 * Exception thrown when a user attempts to access a shipment they do not own.
 */
public class ShipmentOwnershipException extends RuntimeException {
    public ShipmentOwnershipException(String message) {
        super(message);
    }
}
