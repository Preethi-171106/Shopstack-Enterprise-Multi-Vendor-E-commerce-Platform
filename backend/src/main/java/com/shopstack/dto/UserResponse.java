package com.shopstack.dto;

import com.shopstack.entity.UserRole;
import java.time.LocalDateTime;

/**
 * UserResponse — Data Transfer Object (DTO) for returning user details in API responses.
 *
 * <p><b>Security Note:</b><br>
 * This DTO explicitly excludes password and password hash fields to prevent sensitive credential leakage.
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UserRole role,
        boolean enabled,
        LocalDateTime createdAt
) {
}
