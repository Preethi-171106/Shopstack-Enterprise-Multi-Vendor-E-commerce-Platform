package com.shopstack.config;

import com.shopstack.security.CustomAccessDeniedHandler;
import com.shopstack.security.CustomAuthenticationEntryPoint;
import com.shopstack.security.CustomUserDetailsService;
import com.shopstack.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * SecurityConfig — Main Spring Security Configuration for stateless JWT REST API with Role-Based Access Control (RBAC).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            CustomUserDetailsService userDetailsService,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler,
            PasswordEncoder passwordEncoder
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF since REST APIs use stateless JWT authentication
                .csrf(AbstractHttpConfigurer::disable)
                // Enable CORS integration with CorsConfig
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Swagger/OpenAPI documentation endpoints
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // Public endpoints
                        .requestMatchers("/api/health", "/api/auth/register", "/api/auth/register/**", "/api/auth/login").permitAll()
                        // Password reset endpoints (public — no JWT required)
                        .requestMatchers("/api/auth/forgot-password",
                                         "/api/auth/reset-password",
                                         "/api/auth/reset-password/validate").permitAll()
                        // Public category endpoints (no JWT required)
                        .requestMatchers("/api/categories", "/api/categories/**").permitAll()
                        // Public product endpoints (no JWT required) — active products only
                        .requestMatchers("/api/products", "/api/products/**").permitAll()
                        // Warehouse physical facilities read access for Staff & Admin
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/admin/warehouses", "/api/admin/warehouses/**").hasAnyRole("WAREHOUSE_STAFF", "ADMIN")
                        // Admin-wide endpoints (ROLE_ADMIN only)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Notifications — all authenticated users
                        .requestMatchers("/api/notifications", "/api/notifications/**").authenticated()
                        // Authenticated endpoint (any valid role)
                        .requestMatchers("/api/auth/me").authenticated()
                        // Role-specific test endpoints
                        .requestMatchers("/api/test/customer").hasRole("CUSTOMER")
                        .requestMatchers("/api/test/vendor").hasRole("VENDOR")
                        .requestMatchers("/api/test/admin").hasRole("ADMIN")
                        .requestMatchers("/api/test/warehouse").hasRole("WAREHOUSE_STAFF")
                        // Vendor management endpoints (ROLE_VENDOR)
                        .requestMatchers("/api/vendors/**").hasRole("VENDOR")
                        // Cart & Wishlist endpoints (authenticated users only)
                        .requestMatchers("/api/cart", "/api/cart/**").authenticated()
                        .requestMatchers("/api/wishlist", "/api/wishlist/**").authenticated()
                        // Order endpoints — CUSTOMER for create/view own orders; ADMIN for all orders
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/orders/admin").hasRole("ADMIN")
                        .requestMatchers("/api/orders", "/api/orders/**").authenticated()
                        // Returns endpoints (role enforcement via @PreAuthorize in ReturnController)
                        .requestMatchers("/api/returns", "/api/returns/**").authenticated()
                        // Warehouse Staff & Admin shipment & inventory management
                        .requestMatchers("/api/warehouse", "/api/warehouse/**").hasAnyRole("WAREHOUSE_STAFF", "ADMIN")
                        // Customer shipment tracking (own orders only)
                        .requestMatchers("/api/customer/shipments", "/api/customer/shipments/**").hasRole("CUSTOMER")
                        // Vendor shipment & inventory visibility (own products only)
                        .requestMatchers("/api/vendor/shipments", "/api/vendor/shipments/**").hasRole("VENDOR")
                        .requestMatchers("/api/vendor/inventory", "/api/vendor/inventory/**").hasRole("VENDOR")
                        // Public tracking number lookup (any authenticated user)
                        .requestMatchers("/api/shipments/tracking/**").authenticated()
                        // Customer Payment endpoints (ROLE_CUSTOMER only)
                        .requestMatchers("/api/payments", "/api/payments/**").hasRole("CUSTOMER")
                        // Customer Coupon endpoints (ROLE_CUSTOMER only)
                        .requestMatchers("/api/coupons", "/api/coupons/**").hasRole("CUSTOMER")
                        // All other API requests require authentication by default
                        .anyRequest().authenticated()
                )
                // Stateless session management (no HTTP session created or used)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Handle unauthenticated (401) & forbidden (403) errors cleanly
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // Set custom authentication provider
                .authenticationProvider(authenticationProvider())
                // Add JWT filter before standard Spring Security authentication filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
