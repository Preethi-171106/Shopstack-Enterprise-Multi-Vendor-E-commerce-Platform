package com.shopstack.dto.category;

import jakarta.validation.constraints.NotNull;

/**
 * CategoryStatusRequest — Input DTO for toggling a category's active status.
 *
 * <p>Used by the PATCH /api/admin/categories/{id}/status endpoint.
 * Sends either {@code {"active": true}} or {@code {"active": false}}.
 *
 * <p>This is a soft status change — the category record is preserved in the database
 * but hidden from public endpoints when active is set to false.
 */
public record CategoryStatusRequest(

        @NotNull(message = "Active status is required")
        Boolean active
) {
}
