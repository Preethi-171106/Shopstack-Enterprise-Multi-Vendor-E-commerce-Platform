package com.shopstack.controller;

import com.shopstack.dto.commission.CommissionResponse;
import com.shopstack.dto.commission.CommissionSummaryResponse;
import com.shopstack.dto.commission.VendorRevenueResponse;
import com.shopstack.service.CommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * CommissionController — REST endpoints for commission and revenue data.
 */
@RestController
@Tag(name = "Commissions", description = "Vendor commission and revenue endpoints")
public class CommissionController {

    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping("/api/admin/commissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all commission records (ADMIN only)")
    public ResponseEntity<List<CommissionResponse>> getAllCommissions() {
        return ResponseEntity.ok(commissionService.getAllCommissions());
    }

    @GetMapping("/api/admin/commissions/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get platform commission summary and vendor breakdown (ADMIN only)")
    public ResponseEntity<CommissionSummaryResponse> getCommissionSummary() {
        return ResponseEntity.ok(commissionService.getCommissionSummary());
    }

    @GetMapping("/api/admin/commissions/total")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get total platform commission earned (ADMIN only)")
    public ResponseEntity<BigDecimal> getTotalPlatformCommission() {
        return ResponseEntity.ok(commissionService.getTotalPlatformCommission());
    }

    // ── Vendor endpoints ──────────────────────────────────────────────────────

    @GetMapping("/api/vendors/commissions")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "List my commission records (VENDOR only)")
    public ResponseEntity<List<CommissionResponse>> getMyCommissions() {
        return ResponseEntity.ok(commissionService.getMyCommissions());
    }

    @GetMapping("/api/vendors/revenue")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get my revenue summary (VENDOR only)")
    public ResponseEntity<VendorRevenueResponse> getMyRevenue() {
        return ResponseEntity.ok(commissionService.getMyRevenue());
    }
}
