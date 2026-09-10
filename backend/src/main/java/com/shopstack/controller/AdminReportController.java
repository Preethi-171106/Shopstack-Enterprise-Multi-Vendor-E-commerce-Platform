package com.shopstack.controller;

import com.shopstack.dto.report.*;
import com.shopstack.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AdminReportController — REST endpoints for generating business reports and CSV export.
 *
 * <p>All endpoints require {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Business Reports", description = "Endpoints for business reporting and CSV export")
public class AdminReportController {

    private final AdminReportService reportService;

    public AdminReportController(AdminReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    @Operation(summary = "Generate Sales Report (Admin only)")
    public ResponseEntity<SalesReportResponse> getSalesReport() {
        return ResponseEntity.ok(reportService.generateSalesReport());
    }

    @GetMapping("/orders")
    @Operation(summary = "Generate Orders Report (Admin only)")
    public ResponseEntity<OrderReportResponse> getOrderReport() {
        return ResponseEntity.ok(reportService.generateOrderReport());
    }

    @GetMapping("/vendors")
    @Operation(summary = "Generate Vendor Performance Report (Admin only)")
    public ResponseEntity<VendorReportResponse> getVendorReport() {
        return ResponseEntity.ok(reportService.generateVendorReport());
    }

    @GetMapping("/commissions")
    @Operation(summary = "Generate Commission Report (Admin only)")
    public ResponseEntity<CommissionReportResponse> getCommissionReport() {
        return ResponseEntity.ok(reportService.generateCommissionReport());
    }

    @GetMapping("/products")
    @Operation(summary = "Generate Product & Inventory Report (Admin only)")
    public ResponseEntity<ProductReportResponse> getProductReport() {
        return ResponseEntity.ok(reportService.generateProductReport());
    }

    @GetMapping("/export")
    @Operation(summary = "Export Business Report as CSV (Admin only)")
    public ResponseEntity<byte[]> exportReportQuery(@RequestParam(defaultValue = "orders") String type) {
        return buildCsvResponse(type);
    }

    @GetMapping("/{type}/export")
    @Operation(summary = "Export Specific Business Report as CSV (Admin only)")
    public ResponseEntity<byte[]> exportReportPath(@PathVariable String type) {
        return buildCsvResponse(type);
    }

    private ResponseEntity<byte[]> buildCsvResponse(String type) {
        byte[] csvData = reportService.exportCsv(type);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("shopstack_%s_report_%s.csv", type.toLowerCase().trim(), timestamp);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
