package com.shopstack.controller;

import com.shopstack.dto.analytics.AdminDashboardStats;
import com.shopstack.dto.analytics.MarketplaceTrendsResponse;
import com.shopstack.dto.analytics.VendorDashboardStats;
import com.shopstack.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AnalyticsController — real-data analytics and dashboard metrics.
 */
@RestController
@Tag(name = "Analytics", description = "Dashboard analytics and reporting endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Admin Dashboard statistics (GET /api/admin/dashboard and /api/admin/analytics/dashboard)
     */
    @GetMapping({"/api/admin/dashboard", "/api/admin/analytics/dashboard"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin dashboard summary statistics (ADMIN only)")
    public ResponseEntity<AdminDashboardStats> getAdminStats() {
        return ResponseEntity.ok(analyticsService.getAdminStats());
    }

    /**
     * Admin Marketplace Trends and performance analytics.
     */
    @GetMapping("/api/admin/analytics/trends")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marketplace sales and order trends (ADMIN only)")
    public ResponseEntity<MarketplaceTrendsResponse> getMarketplaceTrends() {
        return ResponseEntity.ok(analyticsService.getMarketplaceTrends());
    }

    /**
     * Vendor Dashboard statistics
     */
    @GetMapping("/api/vendors/analytics/dashboard")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor dashboard statistics (VENDOR only)")
    public ResponseEntity<VendorDashboardStats> getVendorStats() {
        return ResponseEntity.ok(analyticsService.getVendorStats());
    }
}
