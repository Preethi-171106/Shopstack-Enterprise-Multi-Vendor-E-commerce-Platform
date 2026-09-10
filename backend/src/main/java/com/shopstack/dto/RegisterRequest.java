package com.shopstack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * RegisterRequest — Data Transfer Object (DTO) for public registration requests.
 *
 * <p><b>Security Design:</b><br>
 * Only {@code CUSTOMER} and {@code VENDOR} are accepted as {@code registrationRole}.
 * Any attempt to register as {@code ADMIN} or {@code WAREHOUSE_STAFF} is rejected
 * with HTTP 400 in {@link com.shopstack.service.AuthService}.  The backend never
 * trusts the frontend role field blindly — it validates it before use.
 *
 * <p>If {@code registrationRole} is {@code null} or blank it defaults to
 * {@code CUSTOMER} in the service layer, preserving backward compatibility.
 */
public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address format")
        @Size(max = 150, message = "Email cannot exceed 150 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        @Pattern(regexp = "^$|^[0-9+\\-\\s()]{7,20}$", message = "Invalid phone number format")
        String phoneNumber,

        /**
         * The account type requested by the registrant.
         *
         * <p>Accepted values (case-insensitive): {@code CUSTOMER}, {@code VENDOR}, {@code ADMIN}.
         * <p>Rejected values: {@code WAREHOUSE_STAFF} — returns HTTP 400.
         *    Warehouse staff accounts are created by administrators only.
         * <p>When {@code null} or blank the service defaults to {@code CUSTOMER}.
         */
        String registrationRole
) {
    /**
     * Compact canonical constructor that normalises the registrationRole to uppercase.
     */
    public RegisterRequest {
        if (registrationRole != null) {
            registrationRole = registrationRole.trim().toUpperCase();
        }
    }
}
