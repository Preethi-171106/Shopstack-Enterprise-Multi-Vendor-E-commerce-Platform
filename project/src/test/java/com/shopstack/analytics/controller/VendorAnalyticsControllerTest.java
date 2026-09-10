package com.shopstack.analytics.controller;

import com.shopstack.analytics.dto.*;
import com.shopstack.analytics.service.AnalyticsService;
import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.common.exception.UnauthorizedActionException;
import com.shopstack.security.JwtAuthEntryPoint;
import com.shopstack.security.JwtAuthenticationFilter;
import com.shopstack.security.JwtTokenProvider;
import com.shopstack.security.SecurityUtils;
import com.shopstack.vendor.entity.Vendor;
import com.shopstack.vendor.repository.VendorRepository;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendorAnalyticsController.class)
@Import({com.shopstack.security.SecurityConfig.class, JwtAuthEntryPoint.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@AutoConfigureMockMvc
class VendorAnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AnalyticsService analyticsService;
    @MockBean private SecurityUtils securityUtils;
    @MockBean private VendorRepository vendorRepository;

    private Vendor vendor;
    private VendorDashboardResponse dashboardResponse;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder().id(1L).storeName("TechStore").active(true).build();
        dashboardResponse = new VendorDashboardResponse(
                "TechStore", 50, 45, 200, 10, 150,
                BigDecimal.valueOf(25000), BigDecimal.valueOf(5000), BigDecimal.valueOf(1000),
                List.of(), List.of(), new InventorySummary(50, 5, 3, 47));

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.hasRole("ROLE_VENDOR")).thenReturn(true);
        when(vendorRepository.findByUserId(1L)).thenReturn(Optional.of(vendor));
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getDashboard_returns200() throws Exception {
        when(analyticsService.getVendorDashboard(1L)).thenReturn(dashboardResponse);

        mockMvc.perform(get("/api/vendor/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("TechStore"))
                .andExpect(jsonPath("$.totalProducts").value(50))
                .andExpect(jsonPath("$.revenue").value(25000));
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getSales_returns200() throws Exception {
        SalesSummaryResponse response = new SalesSummaryResponse(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
                100, BigDecimal.valueOf(10000), BigDecimal.valueOf(100), List.of());
        when(analyticsService.getVendorSales(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/vendor/analytics/sales")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(100));
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getProducts_returns200() throws Exception {
        when(analyticsService.getVendorProducts(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/vendor/analytics/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getOrders_returns200() throws Exception {
        when(analyticsService.getOrderStatusStatisticsByVendor(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/vendor/analytics/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getInventory_returns200() throws Exception {
        InventorySummary summary = new InventorySummary(50, 5, 3, 47);
        when(analyticsService.getVendorInventorySummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/vendor/analytics/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(50));
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getTopProducts_returns200() throws Exception {
        when(analyticsService.getTopSellingProductsByVendor(eq(1L), eq(10))).thenReturn(List.of());

        mockMvc.perform(get("/api/vendor/analytics/top-products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void getDashboard_vendorNotFound_returns404() throws Exception {
        when(vendorRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(analyticsService.getVendorDashboard(any()))
                .thenThrow(new ResourceNotFoundException("Vendor profile not found for user"));

        mockMvc.perform(get("/api/vendor/analytics/dashboard"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void vendorEndpoint_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/vendor/analytics/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendorEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/vendor/analytics/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}
