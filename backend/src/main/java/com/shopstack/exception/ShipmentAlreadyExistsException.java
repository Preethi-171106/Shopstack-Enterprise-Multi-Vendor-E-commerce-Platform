package com.shopstack.exception;

/**
 * Exception thrown when attempting to create a shipment for an order that already has one.
 */
public class ShipmentAlreadyExistsException extends RuntimeException {
    public ShipmentAlreadyExistsException(String message) {
        super(message);
    }
}
