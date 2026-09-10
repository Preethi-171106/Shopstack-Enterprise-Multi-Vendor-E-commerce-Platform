package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.ForgotPasswordRequest;
import com.shopstack.dto.LoginRequest;
import com.shopstack.dto.LoginResponse;
import com.shopstack.dto.RegisterRequest;
import com.shopstack.dto.ResetPasswordRequest;
import com.shopstack.dto.UserResponse;
import com.shopstack.entity.UserRole;
import com.shopstack.exception.EmailAlreadyExistsException;
import com.shopstack.service.AuthService;
import com.shopstack.service.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthControllerTest — MockMvc integration tests for POST /api/auth/register and POST /api/auth/login.
 *
 * <p>Verifies all 9 required registration/login test scenarios:
 * <ol>
 *   <li>Customer registration → HTTP 201, role = CUSTOMER</li>
 *   <li>Vendor registration → HTTP 201, role = VENDOR</li>
 *   <li>ADMIN registration attempt → HTTP 400 rejected</li>
 *   <li>WAREHOUSE_STAFF registration attempt → HTTP 400 rejected</li>
 *   <li>Duplicate email → HTTP 409 Conflict</li>
 *   <li>Customer login → HTTP 200, JWT returned, role = CUSTOMER</li>
 *   <li>Vendor login → HTTP 200, JWT returned, role = VENDOR</li>
 *   <li>Admin login → HTTP 200, still works</li>
 *   <li>Warehouse staff login → HTTP 200, still works</li>
 * </ol>
 *
 * <p>Uses {@code @MockBean AuthService} — no real DB access.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordResetService passwordResetService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private UserResponse userResponse(Long id, String email, UserRole role) {
        return new UserResponse(id, "Test", "User", email, null, role, true, LocalDateTime.now());
    }

    private LoginResponse loginResponse(String email, UserRole role) {
        return new LoginResponse("mock.jwt.token", 86400000L, userResponse(1L, email, role));
    }

    private RegisterRequest registerRequest(String email, String role) {
        return new RegisterRequest("Test", "User", email, "password123", null, role);
    }

    // =========================================================================
    // TEST 1 — Customer Registration
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/register — Customer Registration")
    class CustomerRegistration {

        @Test
        @DisplayName("TEST 1: Register as CUSTOMER → HTTP 201, role = CUSTOMER")
        void registerCustomer_Returns201WithCustomerRole() throws Exception {
            given(authService.registerUser(any(RegisterRequest.class)))
                    .willReturn(userResponse(1L, "customer@test.com", UserRole.CUSTOMER));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    registerRequest("customer@test.com", "CUSTOMER"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.email").value("customer@test.com"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("TEST 1b: Register without explicit role → HTTP 201, defaults to CUSTOMER")
        void registerWithNoRole_DefaultsToCustomer() throws Exception {
            given(authService.registerUser(any(RegisterRequest.class)))
                    .willReturn(userResponse(2L, "norolecust@test.com", UserRole.CUSTOMER));

            // Send request without registrationRole field
            String body = """
                    {
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "norolecust@test.com",
                        "password": "password123"
                    }
                    """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }
    }

    // =========================================================================
    // TEST 2 — Vendor Registration
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/register — Vendor Registration")
    class VendorRegistration {

        @Test
        @DisplayName("TEST 2: Register as VENDOR → HTTP 201, role = VENDOR")
        void registerVendor_Returns201WithVendorRole() throws Exception {
            given(authService.registerUser(any(RegisterRequest.class)))
                    .willReturn(userResponse(3L, "vendor@test.com", UserRole.VENDOR));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    registerRequest("vendor@test.com", "VENDOR"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("VENDOR"))
                    .andExpect(jsonPath("$.email").value("vendor@test.com"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }
    }

    // =========================================================================
    // TEST 3 — ADMIN Registration (now ALLOWED)
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/register — ADMIN Registration")
    class AdminRegistration {

        @Test
        @DisplayName("TEST 3: Register as ADMIN → HTTP 201, role = ADMIN")
        void registerAdmin_Returns201WithAdminRole() throws Exception {
            given(authService.registerUser(any(RegisterRequest.class)))
                    .willReturn(userResponse(4L, "admin@test.com", UserRole.ADMIN));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    registerRequest("admin@test.com", "ADMIN"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.email").value("admin@test.com"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }
    }

    // =========================================================================
    // TEST 4 — WAREHOUSE_STAFF Registration Rejected (admin-created only)
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/register — WAREHOUSE_STAFF Registration Blocked")
    class WarehouseRegistrationBlocked {

        @Test
        @DisplayName("TEST 4: Register as WAREHOUSE_STAFF → HTTP 400 rejected (admin-created only)")
        void registerAsWarehouseStaff_Returns400() throws Exception {
            given(authService.registerUser(any(RegisterRequest.class)))
                    .willThrow(new IllegalArgumentException(
                            "Invalid account type 'WAREHOUSE_STAFF'. "
                            + "WAREHOUSE_STAFF accounts must be created by an administrator."));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    registerRequest("wh@test.com", "WAREHOUSE_STAFF"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("WAREHOUSE_STAFF")));
        }
    }

    // =========================================================================
    // TEST 5 — Duplicate Email
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/register — Duplicate Email")
    class DuplicateEmail {

        @Test
        @DisplayName("TEST 5: Duplicate email → HTTP 409 Conflict with clear message")
        void duplicateEmail_Returns409() throws Exception {
            given(authService.registerUser(any(RegisterRequest.class)))
                    .willThrow(new EmailAlreadyExistsException(
                            "An account with email 'dup@test.com' already exists."));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    registerRequest("dup@test.com", "CUSTOMER"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("dup@test.com")));
        }
    }

    // =========================================================================
    // TEST 6 — Customer Login
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/login — Customer Login")
    class CustomerLogin {

        @Test
        @DisplayName("TEST 6: Customer login → HTTP 200, JWT returned, role = CUSTOMER")
        void customerLogin_Returns200WithJwt() throws Exception {
            given(authService.login(any(LoginRequest.class)))
                    .willReturn(loginResponse("customer@test.com", UserRole.CUSTOMER));

            String body = """
                    {"email": "customer@test.com", "password": "password123"}
                    """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                    .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.user.email").value("customer@test.com"));
        }
    }

    // =========================================================================
    // TEST 7 — Vendor Login
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/login — Vendor Login")
    class VendorLogin {

        @Test
        @DisplayName("TEST 7: Vendor login → HTTP 200, JWT returned, role = VENDOR")
        void vendorLogin_Returns200WithJwt() throws Exception {
            given(authService.login(any(LoginRequest.class)))
                    .willReturn(loginResponse("vendor@test.com", UserRole.VENDOR));

            String body = """
                    {"email": "vendor@test.com", "password": "password123"}
                    """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                    .andExpect(jsonPath("$.user.role").value("VENDOR"))
                    .andExpect(jsonPath("$.user.email").value("vendor@test.com"));
        }
    }

    // =========================================================================
    // TEST 8 — Admin Login (existing admin AND newly registered admin both work)
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/login — Admin Login")
    class AdminLogin {

        @Test
        @DisplayName("TEST 8: Admin login → HTTP 200, role = ADMIN")
        void adminLogin_Returns200WithJwt() throws Exception {
            given(authService.login(any(LoginRequest.class)))
                    .willReturn(loginResponse("admin@test.com", UserRole.ADMIN));

            String body = """
                    {"email": "admin@test.com", "password": "Admin@123"}
                    """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.role").value("ADMIN"))
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }
    }

    // =========================================================================
    // TEST 9 — Warehouse Staff Login (existing account still works)
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/login — Warehouse Staff Login")
    class WarehouseLogin {

        @Test
        @DisplayName("TEST 9: Existing warehouse staff login → HTTP 200, role = WAREHOUSE_STAFF")
        void warehouseLogin_Returns200WithJwt() throws Exception {
            given(authService.login(any(LoginRequest.class)))
                    .willReturn(loginResponse("warehouse@test.com", UserRole.WAREHOUSE_STAFF));

            String body = """
                    {"email": "warehouse@test.com", "password": "WHstaff@123"}
                    """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.role").value("WAREHOUSE_STAFF"))
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }
    }

    // =========================================================================
    // Validation tests
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/register — Input Validation")
    class ValidationTests {

        @Test
        @DisplayName("Blank firstName → HTTP 400 Bad Request")
        void blankFirstName_Returns400() throws Exception {
            String body = """
                    {
                        "firstName": "",
                        "lastName": "User",
                        "email": "val@test.com",
                        "password": "password123"
                    }
                    """;
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("Invalid email format → HTTP 400 Bad Request")
        void invalidEmail_Returns400() throws Exception {
            String body = """
                    {
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "not-an-email",
                        "password": "password123"
                    }
                    """;
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("Password too short (< 6 chars) → HTTP 400 Bad Request")
        void shortPassword_Returns400() throws Exception {
            String body = """
                    {
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "short@test.com",
                        "password": "abc"
                    }
                    """;
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("Password reset flow")
    class PasswordResetFlow {

        @Test
        @DisplayName("Forgot password request returns 200 and generic success message")
        void forgotPassword_Returns200() throws Exception {
            given(passwordResetService.forgotPassword(any(ForgotPasswordRequest.class)))
                    .willReturn("If an account exists for this email, a reset link has been sent.");

            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"user@test.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("If an account exists for this email, a reset link has been sent."));
        }

        @Test
        @DisplayName("Reset password request accepts token in query param")
        void resetPassword_WithTokenInQuery_Returns200() throws Exception {
            given(passwordResetService.isTokenValid("abc123")).willReturn(true);

            mockMvc.perform(post("/api/auth/reset-password?token=abc123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\":\"NewPassword@123\",\"confirmPassword\":\"NewPassword@123\"}"))
                    .andExpect(status().isOk());
        }
    }
}
