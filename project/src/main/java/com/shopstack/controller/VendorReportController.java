package com.shopstack.controller;

import com.shopstack.dto.*;
import com.shopstack.service.ReportService;
import com.shopstack.util.CsvExporter;
import com.shopstack.util.ExcelExporter;
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
@RequestMapping("/api/vendor/reports")
public class VendorReportController {

    private final ReportService reportService;

    public VendorReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<VendorReportDto>> overview(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, null, null, null);
        VendorReportDto report = reportService.vendorSelfReport(filter, ReportService.presetFromString(preset));
        return ResponseEntity.ok(ApiResponse.success("Vendor self report generated", report));
    }

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<SalesReportDto>> sales(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, null, null, null);
        SalesReportDto report = reportService.vendorSelfSalesReport(filter, ReportService.presetFromString(preset));
        return ResponseEntity.ok(ApiResponse.success("Vendor sales report generated", report));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<ProductReportDto>> products(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String preset) {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, null, categoryId, null);
        ProductReportDto report = reportService.vendorSelfProductReport(filter, ReportService.presetFromString(preset));
        return ResponseEntity.ok(ApiResponse.success("Vendor product report generated", report));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<InventoryReportDto>> inventory(
            @RequestParam(required = false) Long categoryId) {
        ReportFilter filter = reportService.normalizeFilter(null, null, null, categoryId, null);
        InventoryReportDto report = reportService.vendorSelfInventoryReport(filter);
        return ResponseEntity.ok(ApiResponse.success("Vendor inventory report generated", report));
    }

    @GetMapping("/export/csv")
    public void exportCsv(
            @RequestParam ReportType type,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String preset,
            HttpServletResponse response) throws IOException {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, null, categoryId, null);
        ReportTabulator.Tabular tabular = vendorTabulate(type, filter, preset);
        String csv = CsvExporter.export(tabular.headers(), tabular.rows());
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"vendor-" + type.name().toLowerCase() + "-report.csv\"");
        response.getWriter().write(csv);
        response.getWriter().flush();
    }

    @GetMapping("/export/excel")
    public void exportExcel(
            @RequestParam ReportType type,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String preset,
            HttpServletResponse response) throws IOException {
        ReportFilter filter = reportService.normalizeFilter(fromDate, toDate, null, categoryId, null);
        ReportTabulator.Tabular tabular = vendorTabulate(type, filter, preset);
        byte[] xlsx = ExcelExporter.export(tabular.sheetName(), tabular.headers(), tabular.rows());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"vendor-" + type.name().toLowerCase() + "-report.xlsx\"");
        response.setContentLength(xlsx.length);
        try (OutputStream out = response.getOutputStream()) {
            out.write(xlsx);
            out.flush();
        }
    }

    private ReportTabulator.Tabular vendorTabulate(ReportType type, ReportFilter filter, String preset) {
        return switch (type) {
            case SALES -> ReportTabulator.tabulate(type,
                    reportService.vendorSelfSalesReport(filter, ReportService.presetFromString(preset)));
            case VENDORS -> ReportTabulator.tabulate(type,
                    reportService.vendorSelfReport(filter, ReportService.presetFromString(preset)));
            case PRODUCTS -> ReportTabulator.tabulate(type,
                    reportService.vendorSelfProductReport(filter, ReportService.presetFromString(preset)));
            case INVENTORY -> ReportTabulator.tabulate(type,
                    reportService.vendorSelfInventoryReport(filter));
            default -> reportService.tabulate(type, filter, ReportService.presetFromString(preset));
        };
    }
}
