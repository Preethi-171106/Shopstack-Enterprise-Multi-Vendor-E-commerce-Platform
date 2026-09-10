package com.shopstack.support;

import com.shopstack.security.jwt.JwtTokenProvider;

/**
 * Builds short-lived JWTs used as Authorization headers in MockMvc tests.
 * Tokens are generated through the real {@link JwtTokenProvider} so the
 * JwtAuthenticationFilter accepts them end-to-end. Tests that need a token
 * bound to a specific seeded user id should use {@link #bearer(JwtTokenProvider, Long, String)}.
 */
public final class TestAuthTokens {

    private TestAuthTokens() {
    }

    public static String bearer(JwtTokenProvider provider, Long userId, String role) {
        return "Bearer " + provider.generateToken(userId, role.toLowerCase() + "@shopstack.com", role);
    }

    public static String admin(JwtTokenProvider provider, Long userId) {
        return bearer(provider, userId, "ADMIN");
    }

    public static String vendor(JwtTokenProvider provider, Long userId) {
        return bearer(provider, userId, "VENDOR");
    }

    public static String customer(JwtTokenProvider provider, Long userId) {
        return bearer(provider, userId, "CUSTOMER");
    }

    public static String warehouse(JwtTokenProvider provider, Long userId) {
        return bearer(provider, userId, "WAREHOUSE");
    }
}
