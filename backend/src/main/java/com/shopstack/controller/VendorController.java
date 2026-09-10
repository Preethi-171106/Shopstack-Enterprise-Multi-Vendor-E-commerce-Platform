package com.shopstack.controller;

import com.shopstack.dto.VendorProfileCreateRequest;
import com.shopstack.dto.VendorProfileResponse;
import com.shopstack.dto.VendorProfileUpdateRequest;
import com.shopstack.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VendorController — REST Controller handling vendor profile endpoints.
 *
 * <p>Base URL: {@code /api/vendors}
 * <p>All endpoints require {@code ROLE_VENDOR}.
 */
@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    /**
     * Creates a new store profile for the authenticated vendor.
     *
     * <p><b>Endpoint:</b> {@code POST /api/vendors/profile}
     */
    @PostMapping("/profile")
    public ResponseEntity<VendorProfileResponse> createProfile(
            @Valid @RequestBody VendorProfileCreateRequest request,
            Authentication authentication
    ) {
        VendorProfileResponse response = vendorService.createVendorProfile(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the current authenticated vendor's store profile.
     *
     * <p><b>Endpoint:</b> {@code GET /api/vendors/me}
     */
    @GetMapping("/me")
    public ResponseEntity<VendorProfileResponse> getMyProfile(Authentication authentication) {
        VendorProfileResponse response = vendorService.getVendorProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the current authenticated vendor's store profile.
     *
     * <p><b>Endpoint:</b> {@code PUT /api/vendors/me}
     */
    @PutMapping("/me")
    public ResponseEntity<VendorProfileResponse> updateMyProfile(
            @Valid @RequestBody VendorProfileUpdateRequest request,
            Authentication authentication
    ) {
        VendorProfileResponse response = vendorService.updateVendorProfile(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }
}
