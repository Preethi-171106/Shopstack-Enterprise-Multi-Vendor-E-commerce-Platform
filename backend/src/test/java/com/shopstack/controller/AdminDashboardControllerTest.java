package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.admin.AdminSystemHealthResponse;
import com.shopstack.dto.admin.VendorDetailResponse;
import com.shopstack.dto.analytics.AdminDashboardStats;
import com.shopstack.dto.analytics.MarketplaceTrendsResponse;
import com.shopstack.dto.commission.CommissionSummaryResponse;
import com.shopstack.dto.report.*;
import com.shopstack.entity.VendorStatus;
import com.shopstack.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Admin Dashboard and Reporting Modules Tests")
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private HealthService healthService;

    @MockBean
    private CommissionService commissionService;

    @MockBean
    private AdminReportService reportService;

    @Nested
    @DisplayName("Admin Dashboard & Analytics Security & Data")
    class DashboardTests {

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN can access dashboard stats")
        void adminCanAccessDashboardStats() throws Exception {
            AdminDashboardStats stats = AdminDashboardStats.builder()
                    .totalCustomers(10)
                    .totalVendors(5)
                    .totalProducts(50)
                    .totalOrders(100)
                    .totalRevenue(new BigDecimal("50000.00"))
                    .totalPlatformCommission(new BigDecimal("2500.00"))
                    .build();

            when(analyticsService.getAdminStats()).thenReturn(stats);

            mockMvc.perform(get("/api/admin/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCustomers").value(10))
                    .andExpect(jsonPath("$.totalRevenue").value(50000.00));
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER is forbidden from dashboard stats")
        void customerForbiddenFromDashboard() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "vendor@shopstack.com", roles = {"VENDOR"})
        @DisplayName("VENDOR is forbidden from admin dashboard stats")
        void vendorForbiddenFromDashboard() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN can access marketplace trends")
        void adminCanAccessTrends() throws Exception {
            MarketplaceTrendsResponse trends = MarketplaceTrendsResponse.builder()
                    .dailySalesTrend(List.of(new MarketplaceTrendsResponse.DailyTrend("2026-08-19", new BigDecimal("1500.00"), 3)))
                    .orderStatusDistribution(Map.of("DELIVERED", 10L, "PENDING", 2L))
                    .build();

            when(analyticsService.getMarketplaceTrends()).thenReturn(trends);

            mockMvc.perform(get("/api/admin/analytics/trends"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dailySalesTrend[0].date").value("2026-08-19"));
        }
    }

    @Nested
    @DisplayName("Vendor Management")
    class VendorManagementTests {

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN can get vendor details with live metrics")
        void adminCanGetVendorDetails() throws Exception {
            VendorDetailResponse detail = VendorDetailResponse.builder()
                    .id(1L)
                    .storeName("Tech World")
                    .status(VendorStatus.APPROVED)
                    .totalProducts(12)
                    .totalGrossSales(new BigDecimal("12000.00"))
                    .totalCommissionPaid(new BigDecimal("600.00"))
                    .totalNetEarnings(new BigDecimal("11400.00"))
                    .build();

            when(adminUserService.getVendorDetails(1L)).thenReturn(detail);

            mockMvc.perform(get("/api/admin/vendors/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.storeName").value("Tech World"))
                    .andExpect(jsonPath("$.totalGrossSales").value(12000.00));
        }
    }

    @Nested
    @DisplayName("Commission Management")
    class CommissionTests {

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN can view commission summary")
        void adminCanGetCommissionSummary() throws Exception {
            CommissionSummaryResponse summary = CommissionSummaryResponse.builder()
                    .totalPlatformCommission(new BigDecimal("5000.00"))
                    .totalGrossSales(new BigDecimal("100000.00"))
                    .totalVendorPayouts(new BigDecimal("95000.00"))
                    .defaultCommissionRatePercent(new BigDecimal("5.00"))
                    .build();

            when(commissionService.getCommissionSummary()).thenReturn(summary);

            mockMvc.perform(get("/api/admin/commissions/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPlatformCommission").value(5000.00));
        }
    }

    @Nested
    @DisplayName("System Monitoring")
    class SystemMonitoringTests {

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN can get system health without credentials leaked")
        void adminCanGetSystemHealth() throws Exception {
            AdminSystemHealthResponse health = AdminSystemHealthResponse.builder()
                    .applicationStatus("UP")
                    .databaseStatus("UP")
                    .overallStatus("UP")
                    .uptimeSeconds(3600)
                    .activeThreads(8)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(healthService.getAdminSystemHealth()).thenReturn(health);

            mockMvc.perform(get("/api/admin/system/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.databaseStatus").value("UP"))
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.secret").doesNotExist());
        }

        @Test
        @WithMockUser(username = "customer@shopstack.com", roles = {"CUSTOMER"})
        @DisplayName("CUSTOMER cannot access system health")
        void customerCannotAccessSystemHealth() throws Exception {
            mockMvc.perform(get("/api/admin/system/health"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Business Reports & CSV Export")
    class ReportTests {

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN can generate sales report")
        void adminCanGetSalesReport() throws Exception {
            SalesReportResponse report = SalesReportResponse.builder()
                    .totalGrossRevenue(new BigDecimal("50000.00"))
                    .totalNetRevenue(new BigDecimal("48000.00"))
                    .totalOrdersCount(25)
                    .records(Collections.emptyList())
                    .build();

            when(reportService.generateSalesReport()).thenReturn(report);

            mockMvc.perform(get("/api/admin/reports/sales"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalGrossRevenue").value(50000.00));
        }

        @Test
        @WithMockUser(username = "admin@shopstack.com", roles = {"ADMIN"})
        @DisplayName("ADMIN can export reports to CSV format")
        void adminCanExportCsv() throws Exception {
            String csvMock = "Order Number,Total Amount\nORD-12345,500.00\n";
            when(reportService.exportCsv("orders")).thenReturn(csvMock.getBytes());

            mockMvc.perform(get("/api/admin/reports/export?type=orders"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv"))
                    .andExpect(header().exists("Content-Disposition"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("ORD-12345")));
        }
    }
}
