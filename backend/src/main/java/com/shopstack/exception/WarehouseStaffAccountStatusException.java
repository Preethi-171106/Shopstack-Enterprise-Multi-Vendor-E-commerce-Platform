package com.shopstack.exception;

/**
 * WarehouseStaffAccountStatusException — Thrown when a warehouse staff account is not in ACTIVE status during login.
 */
public class WarehouseStaffAccountStatusException extends RuntimeException {

    public WarehouseStaffAccountStatusException(String message) {
        super(message);
    }
}
