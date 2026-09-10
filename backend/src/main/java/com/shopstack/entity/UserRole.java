package com.shopstack.entity;

/**
 * UserRole — Enum representing system authorization roles.
 *
 * <p>Values:
 * <ul>
 *   <li>{@code CUSTOMER} — Standard e-commerce shopper</li>
 *   <li>{@code VENDOR} — Seller managing products and store orders</li>
 *   <li>{@code ADMIN} — Platform superuser</li>
 *   <li>{@code WAREHOUSE_STAFF} — Logistics & fulfillment manager</li>
 * </ul>
 */
public enum UserRole {
    CUSTOMER,
    VENDOR,
    ADMIN,
    WAREHOUSE_STAFF
}
