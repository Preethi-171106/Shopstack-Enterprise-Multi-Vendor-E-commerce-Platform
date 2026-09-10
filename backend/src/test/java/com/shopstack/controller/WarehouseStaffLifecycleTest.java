package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.LoginRequest;
import com.shopstack.dto.LoginResponse;
import com.shopstack.dto.UserResponse;
import com.shopstack.dto.warehouse.WarehouseStaffDecisionRequest;
import com.shopstack.dto.warehouse.WarehouseStaffProfileResponse;
import com.shopstack.dto.warehouse.WarehouseStaffRegisterRequest;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.WarehouseStaffStatus;
import com.shopstack.exception.EmailAlreadyExistsException;
import com.shopstack.exception.WarehouseStaffAccountStatusException;
import com.shopstack.service.AdminUserService;
import com.shopstack.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WarehouseStaffLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private AdminUserService adminUserService;

    // =========================================================================
    // 1. PUBLIC REGISTRATION ENDPOINT TESTS
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/register/warehouse-staff — Public Staff Registration")
    class PublicStaffRegistration {

        @Test
        @DisplayName("Success: Register staff → HTTP 201 with PENDING status")
        void registerWarehouseStaff_Success() throws Exception {
            WarehouseStaffRegisterRequest request = new WarehouseStaffRegisterRequest();
            request.setFirstName("Arun");
            request.setLastName("Kumar");
            request.setEmail("arun.staff@warehouse.shopstack.com");
            request.setPassword("SecureStaffPass123!");
            request.setPhoneNumber("+91 9876543210");
            request.setWarehouseId(1L);

            WarehouseStaffProfileResponse response = WarehouseStaffProfileResponse.builder()
                    .id(101L)
                    .userId(201L)
                    .firstName("Arun")
                    .lastName("Kumar")
                    .email("arun.staff@warehouse.shopstack.com")
                    .phoneNumber("+91 9876543210")
                    .warehouseId(1L)
                    .warehouseCode("WH-NORTH-01")
                    .warehouseName("North Regional Fulfillment Center")
                    .status(WarehouseStaffStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(authService.registerWarehouseStaff(any(WarehouseStaffRegisterRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/auth/register/warehouse-staff")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.email").value("arun.staff@warehouse.shopstack.com"))
                    .andExpect(jsonPath("$.warehouseCode").value("WH-NORTH-01"));
        }

        @Test
        @DisplayName("Validation Error: Missing required first name or invalid email → HTTP 400")
        void registerWarehouseStaff_ValidationError() throws Exception {
            WarehouseStaffRegisterRequest invalidRequest = new WarehouseStaffRegisterRequest();
            invalidRequest.setFirstName("");
            invalidRequest.setEmail("not-an-email");
            invalidRequest.setPassword("short");

            mockMvc.perform(post("/api/auth/register/warehouse-staff")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Conflict: Duplicate email address → HTTP 409 Conflict")
        void registerWarehouseStaff_DuplicateEmail_Conflict() throws Exception {
            WarehouseStaffRegisterRequest request = new WarehouseStaffRegisterRequest();
            request.setFirstName("Duplicate");
            request.setLastName("Staff");
            request.setEmail("existing.staff@shopstack.com");
            request.setPassword("SecureStaffPass123!");

            given(authService.registerWarehouseStaff(any(WarehouseStaffRegisterRequest.class)))
                    .willThrow(new EmailAlreadyExistsException("An account with this email already exists"));

            mockMvc.perform(post("/api/auth/register/warehouse-staff")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("An account with this email already exists"));
        }
    }

    // =========================================================================
    // 2. LOGIN STATUS GUARDS
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/login — Warehouse Staff Lifecycle Guard Checks")
    class LoginLifecycleGuards {

        @Test
        @DisplayName("Blocked: Pending staff login attempt → HTTP 403 Forbidden with approval notice")
        void login_PendingStaff_Blocked403() throws Exception {
            LoginRequest request = new LoginRequest("pending.staff@warehouse.shopstack.com", "SecureStaffPass123!");

            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new WarehouseStaffAccountStatusException(
                            "Your Warehouse Staff account is awaiting administrator approval. You will be able to log in once an admin verifies and activates your profile."));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            "Your Warehouse Staff account is awaiting administrator approval. You will be able to log in once an admin verifies and activates your profile."));
        }

        @Test
        @DisplayName("Blocked: Rejected staff login attempt → HTTP 403 Forbidden with rejection notice")
        void login_RejectedStaff_Blocked403() throws Exception {
            LoginRequest request = new LoginRequest("rejected.staff@warehouse.shopstack.com", "SecureStaffPass123!");

            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new WarehouseStaffAccountStatusException(
                            "Your Warehouse Staff registration was rejected. Reason: Incomplete documentation provided."));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            "Your Warehouse Staff registration was rejected. Reason: Incomplete documentation provided."));
        }

        @Test
        @DisplayName("Blocked: Suspended staff login attempt → HTTP 403 Forbidden with suspension notice")
        void login_SuspendedStaff_Blocked403() throws Exception {
            LoginRequest request = new LoginRequest("suspended.staff@warehouse.shopstack.com", "SecureStaffPass123!");

            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new WarehouseStaffAccountStatusException(
                            "Your Warehouse Staff account is currently suspended. Please reach out to the platform administration."));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            "Your Warehouse Staff account is currently suspended. Please reach out to the platform administration."));
        }

        @Test
        @DisplayName("Allowed: Active staff login → HTTP 200 OK with JWT token and staff role")
        void login_ActiveStaff_Success200() throws Exception {
            LoginRequest request = new LoginRequest("active.staff@warehouse.shopstack.com", "SecureStaffPass123!");

            UserResponse userResp = new UserResponse(
                    205L,
                    "Active",
                    "Staff",
                    "active.staff@warehouse.shopstack.com",
                    null,
                    UserRole.WAREHOUSE_STAFF,
                    true,
                    LocalDateTime.now()
            );

            LoginResponse response = new LoginResponse("mocked.jwt.token.for.active.staff", 86400000L, userResp);

            given(authService.login(any(LoginRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("mocked.jwt.token.for.active.staff"))
                    .andExpect(jsonPath("$.user.role").value("WAREHOUSE_STAFF"))
                    .andExpect(jsonPath("$.user.enabled").value(true));
        }
    }

    // =========================================================================
    // 3. ADMIN REVIEW & LIFECYCLE ENDPOINTS
    // =========================================================================

    @Nested
    @DisplayName("Admin Warehouse Staff Lifecycle Actions")
    class AdminStaffActions {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /api/admin/users/warehouse-staff → Lists all staff profiles")
        void getAllStaffProfiles_Admin_Success() throws Exception {
            WarehouseStaffProfileResponse p1 = WarehouseStaffProfileResponse.builder()
                    .id(1L)
                    .userId(10L)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@warehouse.com")
                    .status(WarehouseStaffStatus.PENDING)
                    .build();

            given(adminUserService.getAllWarehouseStaffProfiles()).willReturn(List.of(p1));

            mockMvc.perform(get("/api/admin/users/warehouse-staff"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("john@warehouse.com"))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PATCH /api/admin/users/{id}/warehouse-staff/approve → Approves staff")
        void approveStaff_Admin_Success() throws Exception {
            WarehouseStaffProfileResponse p = WarehouseStaffProfileResponse.builder()
                    .id(1L)
                    .userId(10L)
                    .status(WarehouseStaffStatus.ACTIVE)
                    .approvedByName("admin@shopstack.com")
                    .approvedAt(LocalDateTime.now())
                    .build();

            given(adminUserService.approveWarehouseStaff(eq(10L), anyString())).willReturn(p);

            mockMvc.perform(patch("/api/admin/users/10/warehouse-staff/approve"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.approvedByName").value("admin@shopstack.com"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PATCH /api/admin/users/{id}/warehouse-staff/reject → Rejects staff with reason")
        void rejectStaff_Admin_Success() throws Exception {
            WarehouseStaffDecisionRequest req = new WarehouseStaffDecisionRequest();
            req.setReason("Incomplete background check");

            WarehouseStaffProfileResponse p = WarehouseStaffProfileResponse.builder()
                    .id(1L)
                    .userId(10L)
                    .status(WarehouseStaffStatus.REJECTED)
                    .rejectionReason("Incomplete background check")
                    .build();

            given(adminUserService.rejectWarehouseStaff(eq(10L), eq("Incomplete background check"), anyString()))
                    .willReturn(p);

            mockMvc.perform(patch("/api/admin/users/10/warehouse-staff/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.rejectionReason").value("Incomplete background check"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PATCH /api/admin/users/{id}/warehouse-staff/suspend → Suspends staff account")
        void suspendStaff_Admin_Success() throws Exception {
            WarehouseStaffProfileResponse p = WarehouseStaffProfileResponse.builder()
                    .id(1L)
                    .userId(10L)
                    .status(WarehouseStaffStatus.SUSPENDED)
                    .build();

            given(adminUserService.suspendWarehouseStaff(eq(10L), anyString())).willReturn(p);

            mockMvc.perform(patch("/api/admin/users/10/warehouse-staff/suspend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUSPENDED"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PATCH /api/admin/users/{id}/warehouse-staff/reactivate → Reactivates staff account")
        void reactivateStaff_Admin_Success() throws Exception {
            WarehouseStaffProfileResponse p = WarehouseStaffProfileResponse.builder()
                    .id(1L)
                    .userId(10L)
                    .status(WarehouseStaffStatus.ACTIVE)
                    .build();

            given(adminUserService.reactivateWarehouseStaff(eq(10L), anyString())).willReturn(p);

            mockMvc.perform(patch("/api/admin/users/10/warehouse-staff/reactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Security: Non-admin role cannot approve staff → HTTP 403 Forbidden")
        void approveStaff_ForbiddenForCustomer() throws Exception {
            mockMvc.perform(patch("/api/admin/users/10/warehouse-staff/approve"))
                    .andExpect(status().isForbidden());
        }
    }
}
