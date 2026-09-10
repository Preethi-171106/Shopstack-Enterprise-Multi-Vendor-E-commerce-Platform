package com.shopstack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest — Data Transfer Object (DTO) for login requests.
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address format")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
