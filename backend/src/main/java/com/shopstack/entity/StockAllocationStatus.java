package com.shopstack.entity;

/**
 * StockAllocationStatus — Lifecycle status of a warehouse order-item allocation.
 *
 * <p>Valid progression:
 * ALLOCATED → PICKED → PACKED → READY_FOR_SHIPMENT
 */
public enum StockAllocationStatus {
    ALLOCATED,
    PICKED,
    PACKED,
    READY_FOR_SHIPMENT
}
