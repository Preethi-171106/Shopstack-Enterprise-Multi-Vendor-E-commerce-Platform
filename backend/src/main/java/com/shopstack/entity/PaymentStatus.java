package com.shopstack.entity;

/**
 * PaymentStatus — Enum representing the lifecycle status of a payment transaction.
 */
public enum PaymentStatus {
    PENDING,
    CREATED,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED
}
