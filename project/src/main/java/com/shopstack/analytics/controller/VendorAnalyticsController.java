package com.shopstack.analytics.controller;

import com.shopstack.analytics.dto.*;
import com.shopstack.analytics.service.AnalyticsService;
import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.common.exception.UnauthorizedActionException;
import com.shopstack.security.SecurityUtils;
import com.shopstack.vendor.entity.Vendor;
import com.shopstack.vendor.repository.VendorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vendor/analytics")
public class VendorAnalyticsController {

    private final AnalyticsService analyticsService;
    private final SecurityUtils securityUtils;
    private final VendorRepository vendorRepository;

    public VendorAnalyticsController(AnalyticsService analyticsService,
                                     SecurityUtils securityUtils,
                                     VendorRepository vendorRepository) {
        this.analyticsService = analyticsService;
        this.securityUtils = securityUtils;
        this.vendorRepository = vendorRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<VendorDashboardResponse> getDashboard() {
        Long vendorId = getCurrentVendorId();
        return ResponseEntity.ok(analyticsService.getVendorDashboard(vendorId));
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesSummaryResponse> getSales(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Long vendorId = getCurrentVendorId();
        return ResponseEntity.ok(analyticsService.getVendorSales(vendorId, startDate, endDate));
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductAnalyticsResponse>> getProducts() {
        Long vendorId = getCurrentVendorId();
        return ResponseEntity.ok(analyticsService.getVendorProducts(vendorId));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderStatusResponse>> getOrders() {
        Long vendorId = getCurrentVendorId();
        return ResponseEntity.ok(analyticsService.getOrderStatusStatisticsByVendor(vendorId));
    }

    @GetMapping("/inventory")
    public ResponseEntity<InventorySummary> getInventory() {
        Long vendorId = getCurrentVendorId();
        return ResponseEntity.ok(analyticsService.getVendorInventorySummary(vendorId));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<ProductAnalyticsResponse>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        Long vendorId = getCurrentVendorId();
        return ResponseEntity.ok(analyticsService.getTopSellingProductsByVendor(vendorId, limit));
    }

    private Long getCurrentVendorId() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedActionException("Authentication required");
        }
        if (!securityUtils.hasRole("ROLE_VENDOR")) {
            throw new UnauthorizedActionException("Only vendors can access vendor analytics");
        }
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found for user"));
        return vendor.getId();
    }
}
