package com.shopstack.exception;

/**
 * Exception thrown when an illegal status transition is attempted on a shipment.
 */
public class InvalidShipmentStatusTransitionException extends RuntimeException {
    public InvalidShipmentStatusTransitionException(String message) {
        super(message);
    }
}
