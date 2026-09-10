package com.shopstack.analytics.controller;

import com.shopstack.analytics.dto.*;
import com.shopstack.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(analyticsService.getAdminDashboard());
    }

    @GetMapping("/sales/daily")
    public ResponseEntity<SalesSummaryResponse> getDailySales(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getDailySales(startDate, endDate));
    }

    @GetMapping("/sales/weekly")
    public ResponseEntity<SalesSummaryResponse> getWeeklySales() {
        return ResponseEntity.ok(analyticsService.getWeeklySales());
    }

    @GetMapping("/sales/monthly")
    public ResponseEntity<SalesSummaryResponse> getMonthlySales() {
        return ResponseEntity.ok(analyticsService.getMonthlySales());
    }

    @GetMapping("/sales/yearly")
    public ResponseEntity<SalesSummaryResponse> getYearlySales() {
        return ResponseEntity.ok(analyticsService.getYearlySales());
    }

    @GetMapping("/charts/revenue")
    public ResponseEntity<RevenueChartResponse> getRevenueChart(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getRevenueChart(startDate, endDate));
    }

    @GetMapping("/charts/orders")
    public ResponseEntity<OrdersChartResponse> getOrdersChart(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getOrdersChart(startDate, endDate));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<ProductAnalyticsResponse>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopProducts(limit));
    }

    @GetMapping("/top-categories")
    public ResponseEntity<List<CategoryAnalyticsResponse>> getTopCategories() {
        return ResponseEntity.ok(analyticsService.getTopCategories());
    }

    @GetMapping("/top-vendors")
    public ResponseEntity<List<VendorAnalyticsResponse>> getTopVendors() {
        return ResponseEntity.ok(analyticsService.getTopVendors());
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentAnalyticsResponse>> getPaymentStatistics() {
        return ResponseEntity.ok(analyticsService.getPaymentStatistics());
    }

    @GetMapping("/shipments")
    public ResponseEntity<List<ShipmentStatusResponse>> getShipmentStatistics() {
        return ResponseEntity.ok(analyticsService.getShipmentStatusStatistics());
    }

    @GetMapping("/inventory")
    public ResponseEntity<InventorySummary> getInventorySummary() {
        return ResponseEntity.ok(analyticsService.getAdminInventorySummary());
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<CouponUsageResponse>> getCouponUsageStatistics() {
        return ResponseEntity.ok(analyticsService.getCouponUsageStatistics());
    }
}
