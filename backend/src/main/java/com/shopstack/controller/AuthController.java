package com.shopstack.controller;

import com.shopstack.dto.ForgotPasswordRequest;
import com.shopstack.dto.LoginRequest;
import com.shopstack.dto.LoginResponse;
import com.shopstack.dto.RegisterRequest;
import com.shopstack.dto.ResetPasswordRequest;
import com.shopstack.dto.UserResponse;
import com.shopstack.service.AuthService;
import com.shopstack.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AuthController — REST Controller handling authentication & registration endpoints.
 *
 * <p>Base URL: {@code /api/auth}
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * Registers a new customer or vendor.
     *
     * <p><b>Endpoint:</b> {@code POST /api/auth/register}
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Dedicated Warehouse Staff self-service registration (status = PENDING).
     *
     * <p><b>Endpoint:</b> {@code POST /api/auth/register/warehouse-staff}
     */
    @PostMapping("/register/warehouse-staff")
    public ResponseEntity<com.shopstack.dto.warehouse.WarehouseStaffProfileResponse> registerWarehouseStaff(
            @Valid @RequestBody com.shopstack.dto.warehouse.WarehouseStaffRegisterRequest request) {
        var response = authService.registerWarehouseStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and returns a JWT access token.
     *
     * <p><b>Endpoint:</b> {@code POST /api/auth/login}
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves details of the currently authenticated user.
     *
     * <p><b>Endpoint:</b> {@code GET /api/auth/me} (Protected — requires Bearer token)
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        UserResponse response = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates the forgot-password flow.
     * Always returns the same generic message to prevent email enumeration.
     *
     * <p><b>Endpoint:</b> {@code POST /api/auth/forgot-password} (Public)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        String message = passwordResetService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * Validates a password-reset token without consuming it.
     * Frontend calls this to show a useful error before the user fills in the form.
     *
     * <p><b>Endpoint:</b> {@code GET /api/auth/reset-password/validate?token=...} (Public)
     */
    @GetMapping("/reset-password/validate")
    public ResponseEntity<Map<String, Object>> validateResetToken(
            @RequestParam String token) {
        boolean valid = passwordResetService.isTokenValid(token);
        if (valid) {
            return ResponseEntity.ok(Map.of("valid", true, "message", "Token is valid."));
        } else {
            return ResponseEntity.badRequest().body(
                    Map.of("valid", false, "message",
                           "This password reset link is invalid or has expired. Please request a new one."));
        }
    }

    /**
     * Completes the password-reset flow.
     *
     * <p><b>Endpoint:</b> {@code POST /api/auth/reset-password?token=...} (Public)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestParam(required = false) String token,
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(token, request);
        return ResponseEntity.ok(Map.of(
                "message", "Your password has been reset successfully. You can now sign in."));
    }
}
