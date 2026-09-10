package com.shopstack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * ForgotPasswordRequest — body for POST /api/auth/forgot-password.
 *
 * <p>Only an email is required. The backend never reveals whether the email
 * exists — callers always receive the same generic success message.
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        String email
) {}
