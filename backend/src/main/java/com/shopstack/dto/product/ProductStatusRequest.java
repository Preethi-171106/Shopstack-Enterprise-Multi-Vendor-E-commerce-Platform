package com.shopstack.dto.product;

import jakarta.validation.constraints.NotNull;

/**
 * ProductStatusRequest — DTO for toggling a product's active (visibility) status.
 *
 * <p>Used with:
 * <ul>
 *   <li>{@code PATCH /api/vendors/products/{id}/status}</li>
 *   <li>{@code PATCH /api/admin/products/{id}/status}</li>
 * </ul>
 *
 * <p>When {@code active = false}, the product is soft-deleted — hidden from public
 * listings but preserved in the database for order history and audit purposes.
 *
 * <p>Example request body:
 * <pre>{"active": false}</pre>
 */
public record ProductStatusRequest(

        @NotNull(message = "Active status must be provided (true or false)")
        Boolean active
) {
}
