package com.shopstack.entity;

/**
 * ShipmentStatus — Enum representing the fulfillment lifecycle status of a shipment.
 */
public enum ShipmentStatus {
    PROCESSING,
    READY_TO_SHIP,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    RETURNED
}
