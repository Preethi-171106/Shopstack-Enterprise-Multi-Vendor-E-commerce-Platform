package com.shopstack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ResetPasswordRequest — body for POST /api/auth/reset-password.
 *
 * <p>The token is extracted from the URL query param, not from this body.
 * The body carries the new password and its confirmation only.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String newPassword,

        @NotBlank(message = "Please confirm your new password")
        String confirmPassword
) {}
