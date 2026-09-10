package com.shopstack.security.jwt;

public record AuthenticatedUser(Long userId, String email, String role) {
}
