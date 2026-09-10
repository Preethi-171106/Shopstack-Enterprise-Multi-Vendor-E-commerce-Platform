package com.shopstack.dto;

/**
 * LoginResponse — Data Transfer Object (DTO) returned upon successful authentication.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public LoginResponse(String accessToken, long expiresIn, UserResponse user) {
        this(accessToken, "Bearer", expiresIn, user);
    }
}
