package com.shopstack.entity;

/**
 * OrderStatus — Enum representing the lifecycle status of a customer order.
 *
 * <p>Valid state machine:
 * PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 * PENDING → CANCELLED
 * CONFIRMED → CANCELLED
 * PROCESSING → CANCELLED
 * DELIVERED → RETURN_REQUESTED → RETURNED → REFUNDED
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    REFUNDED
}
