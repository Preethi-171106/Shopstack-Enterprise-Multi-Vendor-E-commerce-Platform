package com.shopstack.controller;

import com.shopstack.dto.CreateStaffUserRequest;
import com.shopstack.dto.UserResponse;
import com.shopstack.dto.VendorProfileResponse;
import com.shopstack.dto.admin.VendorDetailResponse;
import com.shopstack.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AdminUserController — REST Controller for admin user and vendor management.
 *
 * <p>RBAC: {@code ADMIN} role only.
 */
@RestController
@Tag(name = "Admin User Management", description = "Admin endpoints for managing customers and vendors")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Lists all users in the platform.
     * GET /api/admin/users — 200 OK
     */
    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    /**
     * Lists all users filtered by role (CUSTOMER, VENDOR, ADMIN, WAREHOUSE_STAFF).
     * GET /api/admin/users?role=VENDOR — 200 OK
     */
    @GetMapping(value = "/api/admin/users", params = "role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users by role")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@RequestParam String role) {
        return ResponseEntity.ok(adminUserService.getUsersByRole(role));
    }

    /**
     * Gets a single user by ID.
     * GET /api/admin/users/{id} — 200 OK
     */
    @GetMapping("/api/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    /**
     * Enables a user account.
     * PATCH /api/admin/users/{id}/enable — 200 OK
     */
    @PatchMapping("/api/admin/users/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable user account")
    public ResponseEntity<UserResponse> enableUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.setUserEnabled(id, true));
    }

    /**
     * Disables a user account (prevents login).
     * PATCH /api/admin/users/{id}/disable — 200 OK
     */
    @PatchMapping("/api/admin/users/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable user account")
    public ResponseEntity<UserResponse> disableUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.setUserEnabled(id, false));
    }

    /**
     * Lists all vendor profiles with their approval status.
     * GET /api/admin/users/vendors and GET /api/admin/vendors — 200 OK
     */
    @GetMapping({"/api/admin/users/vendors", "/api/admin/vendors"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all vendor profiles")
    public ResponseEntity<List<VendorProfileResponse>> getAllVendorProfiles() {
        return ResponseEntity.ok(adminUserService.getAllVendorProfiles());
    }

    /**
     * Gets full vendor details and sales summary by vendor profile ID.
     * GET /api/admin/users/vendors/{id}/details or GET /api/admin/vendors/{id}
     */
    @GetMapping({"/api/admin/users/vendors/{id}/details", "/api/admin/vendors/{id}"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get vendor details with performance and products")
    public ResponseEntity<VendorDetailResponse> getVendorDetails(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getVendorDetails(id));
    }

    /**
     * Approves a pending vendor profile (sets status to APPROVED).
     * PATCH /api/admin/users/vendors/{vendorProfileId}/approve — 200 OK
     */
    @PatchMapping({"/api/admin/users/vendors/{vendorProfileId}/approve", "/api/admin/vendors/{vendorProfileId}/approve"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve vendor profile")
    public ResponseEntity<VendorProfileResponse> approveVendor(@PathVariable Long vendorProfileId) {
        return ResponseEntity.ok(adminUserService.setVendorStatus(vendorProfileId,
                com.shopstack.entity.VendorStatus.APPROVED));
    }

    /**
     * Rejects a vendor profile (sets status to REJECTED).
     * PATCH /api/admin/users/vendors/{vendorProfileId}/reject — 200 OK
     */
    @PatchMapping({"/api/admin/users/vendors/{vendorProfileId}/reject", "/api/admin/vendors/{vendorProfileId}/reject"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject vendor profile")
    public ResponseEntity<VendorProfileResponse> rejectVendor(@PathVariable Long vendorProfileId) {
        return ResponseEntity.ok(adminUserService.setVendorStatus(vendorProfileId,
                com.shopstack.entity.VendorStatus.REJECTED));
    }

    /**
     * Suspends a vendor profile (sets status to SUSPENDED).
     * PATCH /api/admin/users/vendors/{vendorProfileId}/suspend", "/api/admin/vendors/{vendorProfileId}/suspend"})
     */
    @PatchMapping({"/api/admin/users/vendors/{vendorProfileId}/suspend", "/api/admin/vendors/{vendorProfileId}/suspend"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend vendor profile")
    public ResponseEntity<VendorProfileResponse> suspendVendor(@PathVariable Long vendorProfileId) {
        return ResponseEntity.ok(adminUserService.setVendorStatus(vendorProfileId,
                com.shopstack.entity.VendorStatus.SUSPENDED));
    }

    /**
     * Creates a new WAREHOUSE_STAFF user account. Only ADMIN may call this endpoint.
     * POST /api/admin/users/staff — 201 Created
     */
    @PostMapping("/api/admin/users/staff")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a warehouse staff user (ADMIN only)")
    public ResponseEntity<UserResponse> createWarehouseStaff(
            @Valid @RequestBody CreateStaffUserRequest request) {
        UserResponse created = adminUserService.createWarehouseStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Lists all warehouse staff profiles.
     * GET /api/admin/users/warehouse-staff and GET /api/admin/warehouse-staff — 200 OK
     */
    @GetMapping({"/api/admin/users/warehouse-staff", "/api/admin/warehouse-staff"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all warehouse staff profiles")
    public ResponseEntity<List<com.shopstack.dto.warehouse.WarehouseStaffProfileResponse>> getAllWarehouseStaff() {
        return ResponseEntity.ok(adminUserService.getAllWarehouseStaffProfiles());
    }

    /**
     * Gets a single warehouse staff profile by profile or user ID.
     * GET /api/admin/users/warehouse-staff/{id} and GET /api/admin/warehouse-staff/{id} — 200 OK
     */
    @GetMapping({"/api/admin/users/warehouse-staff/{id}", "/api/admin/warehouse-staff/{id}", "/api/admin/users/{id}/warehouse-staff"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get warehouse staff profile by ID")
    public ResponseEntity<com.shopstack.dto.warehouse.WarehouseStaffProfileResponse> getWarehouseStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getWarehouseStaffProfile(id));
    }

    /**
     * Approves a pending warehouse staff profile (sets status to ACTIVE).
     * PATCH /api/admin/users/warehouse-staff/{id}/approve, PATCH /api/admin/users/{id}/warehouse-staff/approve, and PATCH /api/admin/warehouse-staff/{id}/approve — 200 OK
     */
    @PatchMapping({"/api/admin/users/warehouse-staff/{id}/approve", "/api/admin/users/{id}/warehouse-staff/approve", "/api/admin/warehouse-staff/{id}/approve"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve warehouse staff profile")
    public ResponseEntity<com.shopstack.dto.warehouse.WarehouseStaffProfileResponse> approveWarehouseStaff(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(adminUserService.approveWarehouseStaff(id, adminEmail));
    }

    /**
     * Rejects a warehouse staff application (sets status to REJECTED).
     * PATCH /api/admin/users/warehouse-staff/{id}/reject, PATCH /api/admin/users/{id}/warehouse-staff/reject, and PATCH /api/admin/warehouse-staff/{id}/reject — 200 OK
     */
    @PatchMapping({"/api/admin/users/warehouse-staff/{id}/reject", "/api/admin/users/{id}/warehouse-staff/reject", "/api/admin/warehouse-staff/{id}/reject"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject warehouse staff profile")
    public ResponseEntity<com.shopstack.dto.warehouse.WarehouseStaffProfileResponse> rejectWarehouseStaff(
            @PathVariable Long id,
            @RequestBody(required = false) com.shopstack.dto.warehouse.WarehouseStaffDecisionRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : null;
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(adminUserService.rejectWarehouseStaff(id, reason, adminEmail));
    }

    /**
     * Suspends an active warehouse staff profile (sets status to SUSPENDED).
     * PATCH /api/admin/users/warehouse-staff/{id}/suspend, PATCH /api/admin/users/{id}/warehouse-staff/suspend, and PATCH /api/admin/warehouse-staff/{id}/suspend — 200 OK
     */
    @PatchMapping({"/api/admin/users/warehouse-staff/{id}/suspend", "/api/admin/users/{id}/warehouse-staff/suspend", "/api/admin/warehouse-staff/{id}/suspend"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend warehouse staff profile")
    public ResponseEntity<com.shopstack.dto.warehouse.WarehouseStaffProfileResponse> suspendWarehouseStaff(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(adminUserService.suspendWarehouseStaff(id, adminEmail));
    }

    /**
     * Reactivates a suspended warehouse staff profile (sets status to ACTIVE).
     * PATCH /api/admin/users/warehouse-staff/{id}/reactivate, PATCH /api/admin/users/{id}/warehouse-staff/reactivate, and PATCH /api/admin/warehouse-staff/{id}/reactivate — 200 OK
     */
    @PatchMapping({"/api/admin/users/warehouse-staff/{id}/reactivate", "/api/admin/users/{id}/warehouse-staff/reactivate", "/api/admin/warehouse-staff/{id}/reactivate"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate warehouse staff profile")
    public ResponseEntity<com.shopstack.dto.warehouse.WarehouseStaffProfileResponse> reactivateWarehouseStaff(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(adminUserService.reactivateWarehouseStaff(id, adminEmail));
    }
}
