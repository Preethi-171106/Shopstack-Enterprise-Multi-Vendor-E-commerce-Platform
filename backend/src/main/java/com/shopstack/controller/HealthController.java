package com.shopstack.controller;

import com.shopstack.dto.HealthResponse;
import com.shopstack.dto.admin.AdminSystemHealthResponse;
import com.shopstack.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HealthController — REST Controller for health and operational system monitoring.
 */
@RestController
@Tag(name = "Health & System Monitoring", description = "System availability and diagnostic endpoints")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Public basic health check endpoint.
     * GET /api/health — 200 OK
     */
    @GetMapping("/api/health")
    @Operation(summary = "Public health check")
    public ResponseEntity<HealthResponse> checkHealth() {
        return ResponseEntity.ok(healthService.getHealthStatus());
    }

    /**
     * Admin system & database operational status.
     * GET /api/admin/system/health — 200 OK (ADMIN only)
     */
    @GetMapping("/api/admin/system/health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin detailed system and database health (ADMIN only)")
    public ResponseEntity<AdminSystemHealthResponse> getAdminSystemHealth() {
        return ResponseEntity.ok(healthService.getAdminSystemHealth());
    }
}
