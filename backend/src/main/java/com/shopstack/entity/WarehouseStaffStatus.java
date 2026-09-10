package com.shopstack.entity;

/**
 * WarehouseStaffStatus — Enum representing the administrative approval lifecycle status of a warehouse staff user.
 *
 * <p>Values:
 * <ul>
 *   <li>{@code PENDING} — Registered via public endpoint, awaiting admin review</li>
 *   <li>{@code ACTIVE} — Approved by admin, authorized for warehouse operations</li>
 *   <li>{@code REJECTED} — Registration application declined by admin</li>
 *   <li>{@code SUSPENDED} — Temporarily deactivated by admin</li>
 * </ul>
 */
public enum WarehouseStaffStatus {
    PENDING,
    ACTIVE,
    REJECTED,
    SUSPENDED
}
