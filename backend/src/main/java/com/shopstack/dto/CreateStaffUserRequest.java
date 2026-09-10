package com.shopstack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateStaffUserRequest — DTO for admin-initiated creation of internal staff accounts.
 *
 * <p>Used exclusively by the {@code POST /api/admin/users/staff} endpoint to create
 * {@code WAREHOUSE_STAFF} or other internal role users. Unlike the public
 * {@link RegisterRequest}, the role is controlled by the API endpoint logic —
 * it is NOT supplied by the caller to prevent privilege escalation.
 *
 * <p><b>Security design:</b> The role assigned by this endpoint is fixed at the
 * controller/service level. The caller cannot choose an arbitrary role.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffUserRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address format")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100,
          message = "Password must be between 8 and 100 characters")
    private String password;

    @Pattern(regexp = "^$|^[0-9+\\-\\s()]{7,20}$",
             message = "Invalid phone number format")
    private String phoneNumber;
}
