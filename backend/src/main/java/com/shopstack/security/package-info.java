/**
 * Security package for ShopStack — Spring Security, JWT, and RBAC implementation.
 *
 * <p>Contains:
 * <ul>
 *   <li>{@link com.shopstack.security.JwtService} — JWT generation, parsing, and validation</li>
 *   <li>{@link com.shopstack.security.JwtAuthenticationFilter} — per-request JWT Bearer token filter</li>
 *   <li>{@link com.shopstack.security.CustomUserDetailsService} — Spring Security UserDetailsService adapter</li>
 *   <li>{@link com.shopstack.security.CustomAuthenticationEntryPoint} — 401 Unauthorized JSON response handler</li>
 *   <li>{@link com.shopstack.security.CustomAccessDeniedHandler} — 403 Forbidden JSON response handler</li>
 * </ul>
 *
 * <p>Security is configured in {@link com.shopstack.config.SecurityConfig} with:
 * <ul>
 *   <li>Stateless session management (JWT, no HTTP sessions)</li>
 *   <li>Role-Based Access Control (RBAC) via URL patterns and {@code @PreAuthorize}</li>
 *   <li>BCrypt password hashing</li>
 *   <li>CORS configuration via {@link com.shopstack.config.CorsConfig}</li>
 * </ul>
 */
package com.shopstack.security;
