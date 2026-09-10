package com.shopstack.entity;

/**
 * CommissionStatus — lifecycle states for a vendor commission record.
 */
public enum CommissionStatus {
    /** Commission has been calculated but not yet paid out to the vendor. */
    PENDING,
    /** Commission has been confirmed and payout is being processed. */
    CONFIRMED,
    /** Vendor payout has been completed. */
    PAID,
    /** Order was cancelled/refunded; commission is voided. */
    VOIDED
}
