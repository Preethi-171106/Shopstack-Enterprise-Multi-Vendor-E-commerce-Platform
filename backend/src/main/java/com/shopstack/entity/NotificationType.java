package com.shopstack.entity;

/**
 * NotificationType — categories of in-app notifications across Customer, Vendor, Warehouse Staff, and Admin roles.
 */
public enum NotificationType {
    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_PROCESSING,
    ORDER_SHIPPED,
    ORDER_OUT_FOR_DELIVERY,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    RETURN_REQUESTED,
    RETURN_APPROVED,
    REFUND_PROCESSED,
    LOW_STOCK,
    VENDOR_APPROVED,
    VENDOR_REJECTED,
    VENDOR_SUSPENDED,
    VENDOR_REGISTERED,
    COUPON_AVAILABLE,
    PROMOTION,
    SYSTEM,
    WAREHOUSE_ALERT,
    GENERAL
}

