package com.shopstack.controller;

import com.shopstack.dto.ApiResponse;
import com.shopstack.dto.InventoryReportDto;
import com.shopstack.dto.ReportFilter;
import com.shopstack.service.ReportService;
import com.shopstack.util.CsvExporter;
import com.shopstack.util.ExcelExporter;
import com.shopstack.util.ReportTabulator;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/warehouse/reports")
public class WarehouseReportController {

    private final ReportService reportService;

    public WarehouseReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<InventoryReportDto>> inventory(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId) {
        ReportFilter filter = reportService.normalizeFilter(null, null, vendorId, categoryId, null);
        InventoryReportDto report = reportService.inventoryReport(filter);
        return ResponseEntity.ok(ApiResponse.success("Inventory report generated", report));
    }

    @GetMapping("/inventory/export/csv")
    public void exportCsv(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            HttpServletResponse response) throws IOException {
        ReportFilter filter = reportService.normalizeFilter(null, null, vendorId, categoryId, null);
        InventoryReportDto report = reportService.inventoryReport(filter);
        ReportTabulator.Tabular tabular = ReportTabulator.tabulate(
                com.shopstack.dto.ReportType.INVENTORY, report);
        String csv = CsvExporter.export(tabular.headers(), tabular.rows());
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory-report.csv\"");
        response.getWriter().write(csv);
        response.getWriter().flush();
    }

    @GetMapping("/inventory/export/excel")
    public void exportExcel(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long categoryId,
            HttpServletResponse response) throws IOException {
        ReportFilter filter = reportService.normalizeFilter(null, null, vendorId, categoryId, null);
        InventoryReportDto report = reportService.inventoryReport(filter);
        ReportTabulator.Tabular tabular = ReportTabulator.tabulate(
                com.shopstack.dto.ReportType.INVENTORY, report);
        byte[] xlsx = ExcelExporter.export(tabular.sheetName(), tabular.headers(), tabular.rows());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory-report.xlsx\"");
        response.setContentLength(xlsx.length);
        try (OutputStream out = response.getOutputStream()) {
            out.write(xlsx);
            out.flush();
        }
    }
}
