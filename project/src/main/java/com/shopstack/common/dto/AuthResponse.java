package com.shopstack.common.dto;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String email
) {
}
