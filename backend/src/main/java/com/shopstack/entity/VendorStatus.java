package com.shopstack.entity;

/**
 * VendorStatus — Enum representing the onboarding and approval status of a vendor store profile.
 *
 * <p>Values:
 * <ul>
 *   <li>{@code PENDING} — Initial status when a vendor creates a store profile</li>
 *   <li>{@code APPROVED} — Profile verified and approved by admin</li>
 *   <li>{@code REJECTED} — Profile application rejected by admin</li>
 *   <li>{@code SUSPENDED} — Store suspended due to platform policy violations</li>
 * </ul>
 */
public enum VendorStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED
}
