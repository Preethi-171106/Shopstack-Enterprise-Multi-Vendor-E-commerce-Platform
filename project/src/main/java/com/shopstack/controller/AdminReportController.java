package com.shopstack.controller;

import com.shopstack.dto.*;
import com.shopstack.service.ReportService;
import com.shopstack.util.CsvExporter;
import com.shopstack.util.ExcelExporter;
import com.shopstack.util.ReportDateResolver;
import com.shopstack.util.ReportTabulator;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ---- Sales ----
    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<SalesReportDto>> sales(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, vendorId, categoryId, status);
        SalesReportDto report = reportService.salesReport(filter, ReportService.presetFromString(preset));
        return ResponseEntity.ok(ApiResponse.success("Sales report generated", report));
    }

    // ---- Vendors ----
    @GetMapping("/vendors")
    public ResponseEntity<ApiResponse<VendorReportDto>> vendors(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, vendorId, categoryId, status);
        VendorReportDto report = reportService.vendorReport(filter, ReportService.presetFromString(preset));
        return ResponseEntity.ok(ApiResponse.success("Vendor report generated", report));
    }

    // ---- Products ----
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<ProductReportDto>> products(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, vendorId, categoryId, status);
        ProductReportDto report = reportService.productReport(filter, ReportService.presetFromString(preset));
        return ResponseEntity.ok(ApiResponse.success("Product report generated", report));
    }

    // ---- Customers ----
    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<CustomerReportDto>> customers(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, vendorId, categoryId, status);
        CustomerReportDto report = reportService.customerReport(filter, ReportService.presetFromString(preset));
        return ResponseEntity.ok(ApiResponse.success("Customer report generated", report));
    }

    // ---- Payments ----
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<PaymentReportDto>> payments(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, vendorId, categoryId, status);
        PaymentReportDto report = reportService.paymentReport(filter, ReportService.presetFromString(preset));
        return ResponseEntity.ok(ApiResponse.success("Payment report generated", report));
    }

    // ---- Coupons ----
    @GetMapping("/coupons")
    public ResponseEntity<ApiResponse<CouponReportDto>> coupons(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, vendorId, categoryId, status);
        CouponReportDto report = reportService.couponReport(filter, ReportService.presetFromString(preset));
        return ResponseEntity.ok(ApiResponse.success("Coupon report generated", report));
    }

    // ---- Inventory ----
    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<InventoryReportDto>> inventory(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, vendorId, categoryId, status);
        InventoryReportDto report = reportService.inventoryReport(filter);
        return ResponseEntity.ok(ApiResponse.success("Inventory report generated", report));
    }

    // ---- Exports ----
    @GetMapping("/export/csv")
    public void exportCsv(
            @RequestParam ReportType type,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preset,
            HttpServletResponse response) throws IOException {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, vendorId, categoryId, status);
        ReportTabulator.Tabular tabular = reportService.tabulate(type, filter, ReportService.presetFromString(preset));
        String csv = CsvExporter.export(tabular.headers(), tabular.rows());
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + type.name().toLowerCase() + "-report.csv\"");
        response.getWriter().write(csv);
        response.getWriter().flush();
    }

    @GetMapping("/export/excel")
    public void exportExcel(
            @RequestParam ReportType type,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preset,
            HttpServletResponse response) throws IOException {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, vendorId, categoryId, status);
        ReportTabulator.Tabular tabular = reportService.tabulate(type, filter, ReportService.presetFromString(preset));
        byte[] xlsx = ExcelExporter.export(tabular.sheetName(), tabular.headers(), tabular.rows());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + type.name().toLowerCase() + "-report.xlsx\"");
        response.setContentLength(xlsx.length);
        try (OutputStream out = response.getOutputStream()) {
            out.write(xlsx);
            out.flush();
        }
    }

    @GetMapping(value = "/export/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> exportJson(
            @RequestParam ReportType type,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, vendorId, categoryId, status);
        Object report = switch (type) {
            case SALES -> reportService.salesReport(filter, ReportService.presetFromString(preset));
            case VENDORS -> reportService.vendorReport(filter, ReportService.presetFromString(preset));
            case PRODUCTS -> reportService.productReport(filter, ReportService.presetFromString(preset));
            case CUSTOMERS -> reportService.customerReport(filter, ReportService.presetFromString(preset));
            case PAYMENTS -> reportService.paymentReport(filter, ReportService.presetFromString(preset));
            case COUPONS -> reportService.couponReport(filter, ReportService.presetFromString(preset));
            case INVENTORY -> reportService.inventoryReport(filter);
        };
        return ResponseEntity.ok(ApiResponse.success(type.name().toLowerCase() + " report (json)", report));
    }

    // expose preset enum for clients
    @GetMapping(value = "/presets", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ReportDateResolver.Preset[]>> presets() {
        return ResponseEntity.ok(ApiResponse.success("Available presets", ReportDateResolver.Preset.values()));
    }
}
