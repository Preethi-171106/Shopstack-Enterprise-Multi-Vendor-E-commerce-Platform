package com.shopstack.entity;

/**
 * StockMovementType — Enum representing the reason for a stock quantity change.
 *
 * <ul>
 *   <li>{@code IN} / {@code STOCK_IN} — Stock received from supplier or manual addition</li>
 *   <li>{@code OUT} / {@code STOCK_OUT} — Stock consumed by a fulfilled/shipped order</li>
 *   <li>{@code ADJUSTMENT} — Manual correction (stocktake, write-off, discrepancy)</li>
 *   <li>{@code RESERVED} — Stock set aside for a placed order (not yet fulfilled)</li>
 *   <li>{@code RELEASED} — Reserved stock returned to available pool (order cancelled)</li>
 *   <li>{@code RETURN} — Customer return increases available stock</li>
 *   <li>{@code DAMAGE} — Stock written off due to damage or defect</li>
 *   <li>{@code CORRECTION} — Stock count correction</li>
 * </ul>
 */
public enum StockMovementType {
    IN,
    OUT,
    ADJUSTMENT,
    RESERVED,
    RELEASED,
    RETURN,
    DAMAGE,
    CORRECTION,
    STOCK_IN,
    STOCK_OUT,
    WAREHOUSE_TRANSFER,
    ALLOCATED,
    PICKED,
    PACKED,
    READY_FOR_SHIPMENT,
    RETURN_RECEIVED,
    RETURN_ACCEPTED,
    RETURN_DAMAGED,
    QUARANTINE
}

