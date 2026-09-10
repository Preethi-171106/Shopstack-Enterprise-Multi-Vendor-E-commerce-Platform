package com.shopstack.analytics.controller;

import com.shopstack.analytics.dto.*;
import com.shopstack.analytics.service.AnalyticsService;
import com.shopstack.security.JwtAuthEntryPoint;
import com.shopstack.security.JwtAuthenticationFilter;
import com.shopstack.security.JwtTokenProvider;
import com.shopstack.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminAnalyticsController.class)
@Import({com.shopstack.security.SecurityConfig.class, JwtAuthEntryPoint.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@AutoConfigureMockMvc
class AdminAnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AnalyticsService analyticsService;
    @MockBean private SecurityUtils securityUtils;

    private AdminDashboardResponse dashboardResponse;

    @BeforeEach
    void setUp() {
        dashboardResponse = new AdminDashboardResponse(
                100, 20, 500, 10, 1000, BigDecimal.valueOf(50000), 800,
                50, 700, 100, 30, 25, 15, 5, 3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDashboard_returns200() throws Exception {
        when(analyticsService.getAdminDashboard()).thenReturn(dashboardResponse);

        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(100))
                .andExpect(jsonPath("$.totalRevenue").value(50000));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDailySales_returns200() throws Exception {
        SalesSummaryResponse response = new SalesSummaryResponse(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
                100, BigDecimal.valueOf(10000), BigDecimal.valueOf(100), List.of());
        when(analyticsService.getDailySales(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics/sales/daily")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getWeeklySales_returns200() throws Exception {
        SalesSummaryResponse response = new SalesSummaryResponse(
                LocalDate.now().minusWeeks(1), LocalDate.now(),
                50, BigDecimal.valueOf(5000), BigDecimal.valueOf(100), List.of());
        when(analyticsService.getWeeklySales()).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics/sales/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(50));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMonthlySales_returns200() throws Exception {
        SalesSummaryResponse response = new SalesSummaryResponse(
                LocalDate.now().withDayOfMonth(1), LocalDate.now(),
                200, BigDecimal.valueOf(20000), BigDecimal.valueOf(100), List.of());
        when(analyticsService.getMonthlySales()).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics/sales/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getYearlySales_returns200() throws Exception {
        SalesSummaryResponse response = new SalesSummaryResponse(
                LocalDate.of(2024, 1, 1), LocalDate.now(),
                1000, BigDecimal.valueOf(100000), BigDecimal.valueOf(100), List.of());
        when(analyticsService.getYearlySales()).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics/sales/yearly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(1000));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getRevenueChart_returns200() throws Exception {
        RevenueChartResponse response = new RevenueChartResponse(List.of(), BigDecimal.ZERO);
        when(analyticsService.getRevenueChart(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics/charts/revenue")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrdersChart_returns200() throws Exception {
        OrdersChartResponse response = new OrdersChartResponse(List.of(), 0);
        when(analyticsService.getOrdersChart(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics/charts/orders")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTopProducts_returns200() throws Exception {
        when(analyticsService.getTopProducts(eq(10))).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/top-products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTopCategories_returns200() throws Exception {
        when(analyticsService.getTopCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/top-categories"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTopVendors_returns200() throws Exception {
        when(analyticsService.getTopVendors()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/top-vendors"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getPaymentStatistics_returns200() throws Exception {
        when(analyticsService.getPaymentStatistics()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/payments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getShipmentStatistics_returns200() throws Exception {
        when(analyticsService.getShipmentStatusStatistics()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/shipments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getInventorySummary_returns200() throws Exception {
        InventorySummary summary = new InventorySummary(100, 5, 3, 97);
        when(analyticsService.getAdminInventorySummary()).thenReturn(summary);

        mockMvc.perform(get("/api/admin/analytics/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCouponUsageStatistics_returns200() throws Exception {
        when(analyticsService.getCouponUsageStatistics()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/coupons"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminEndpoint_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}
