package com.shopstack.controller;

import com.shopstack.entity.*;
import com.shopstack.repository.*;
import com.shopstack.security.jwt.JwtTokenProvider;
import com.shopstack.support.TestAuthTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminReportController integration tests")
class AdminReportControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired UserRepository userRepository;
    @Autowired VendorRepository vendorRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired CouponRepository couponRepository;
    @Autowired OrderItemRepository orderItemRepository;

    final ObjectMapper mapper = new ObjectMapper();

    private Long vendorId;
    private Long adminId;

    @BeforeEach
    void seed() {
        User admin = userRepository.save(User.builder()
                .email("admin@shopstack.com").password("x").fullName("Admin").role(UserRole.ADMIN).build());
        adminId = admin.getId();
        User vendorUser = userRepository.save(User.builder()
                .email("vendor@shopstack.com").password("x").fullName("Vendor").role(UserRole.VENDOR)
                .createdAt(LocalDateTime.now().minusDays(10)).build());
        userRepository.save(User.builder()
                .email("cust1@shopstack.com").password("x").fullName("Cust1").role(UserRole.CUSTOMER).build());

        Vendor vendor = vendorRepository.save(Vendor.builder().name("Acme").userId(vendorUser.getId()).build());
        vendorId = vendor.getId();
        Category cat = categoryRepository.save(Category.builder().name("Electronics").build());

        Product p1 = productRepository.save(Product.builder()
                .name("Widget").price(new BigDecimal("50.00")).stockQuantity(2).lowStockThreshold(5)
                .vendor(vendor).category(cat).build());
        Product p2 = productRepository.save(Product.builder()
                .name("Gadget").price(new BigDecimal("30.00")).stockQuantity(0).lowStockThreshold(5)
                .vendor(vendor).category(cat).build());

        Order order = Order.builder()
                .userId(3L).vendorId(vendorId).orderDate(LocalDateTime.now())
                .status(OrderStatus.PAID).totalAmount(new BigDecimal("120.00"))
                .discountAmount(new BigDecimal("10.00"))
                .build();
        OrderItem item = OrderItem.builder()
                .productName("Widget").productId(p1.getId()).quantity(2)
                .unitPrice(new BigDecimal("50.00")).subtotal(new BigDecimal("100.00"))
                .build();
        order.setItems(java.util.List.of(item));
        item.setOrder(order);
        orderRepository.save(order);

        orderRepository.save(Order.builder()
                .userId(3L).vendorId(vendorId).orderDate(LocalDateTime.now())
                .status(OrderStatus.CANCELLED).totalAmount(new BigDecimal("40.00"))
                .discountAmount(BigDecimal.ZERO).build());

        paymentRepository.save(Payment.builder()
                .orderId(1L).userId(3L).amount(new BigDecimal("120.00"))
                .status(PaymentStatus.SUCCESSFUL).paymentMethod("CARD")
                .paymentDate(LocalDateTime.now()).build());
        paymentRepository.save(Payment.builder()
                .orderId(2L).userId(3L).amount(new BigDecimal("40.00"))
                .status(PaymentStatus.FAILED).paymentMethod("PAYPAL")
                .paymentDate(LocalDateTime.now()).build());

        couponRepository.save(Coupon.builder()
                .code("SAVE10").discountPercentage(new BigDecimal("10.00"))
                .maxDiscount(new BigDecimal("100.00")).usageCount(5)
                .totalDiscount(new BigDecimal("50.00")).build());
    }

    private String adminToken() {
        return TestAuthTokens.admin(tokenProvider, adminId);
    }

    @Nested
    @DisplayName("JSON report endpoints")
    class Json {
        @Test
        @DisplayName("GET /api/admin/reports/sales returns sales report for admin")
        void sales() throws Exception {
            mockMvc.perform(get("/api/admin/reports/sales?preset=monthly")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.revenue").exists())
                    .andExpect(jsonPath("$.data.ordersCount").exists());
        }

        @Test
        @DisplayName("GET /api/admin/reports/vendors returns vendor report")
        void vendors() throws Exception {
            mockMvc.perform(get("/api/admin/reports/vendors?preset=monthly")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.vendors").isArray())
                    .andExpect(jsonPath("$.data.topVendors").isArray());
        }

        @Test
        @DisplayName("GET /api/admin/reports/products returns product report with low/out of stock")
        void products() throws Exception {
            mockMvc.perform(get("/api/admin/reports/products?preset=monthly")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lowStockProducts").isArray())
                    .andExpect(jsonPath("$.data.outOfStockProducts").isArray());
        }

        @Test
        @DisplayName("GET /api/admin/reports/customers returns customer report")
        void customers() throws Exception {
            mockMvc.perform(get("/api/admin/reports/customers?preset=monthly")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalCustomers").isNumber())
                    .andExpect(jsonPath("$.data.newCustomers").isNumber());
        }

        @Test
        @DisplayName("GET /api/admin/reports/payments returns payment report")
        void payments() throws Exception {
            mockMvc.perform(get("/api/admin/reports/payments?preset=monthly")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.successfulPayments").isNumber())
                    .andExpect(jsonPath("$.data.failedPayments").isNumber());
        }

        @Test
        @DisplayName("GET /api/admin/reports/coupons returns coupon report")
        void coupons() throws Exception {
            mockMvc.perform(get("/api/admin/reports/coupons?preset=monthly")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalDiscount").exists());
        }

        @Test
        @DisplayName("GET /api/admin/reports/inventory returns inventory report")
        void inventory() throws Exception {
            mockMvc.perform(get("/api/admin/reports/inventory")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalProducts").isNumber())
                    .andExpect(jsonPath("$.data.lowStockCount").isNumber())
                    .andExpect(jsonPath("$.data.outOfStockCount").isNumber());
        }

        @Test
        @DisplayName("custom date range filter is accepted")
        void customRange() throws Exception {
            mockMvc.perform(get("/api/admin/reports/sales?fromDate=2024-01-01&toDate=2024-01-31")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.period.from").value("2024-01-01"));
        }
    }

    @Nested
    @DisplayName("Export endpoints")
    class Export {
        @Test
        @DisplayName("CSV export returns text/csv with attachment header")
        void csvExport() throws Exception {
            mockMvc.perform(get("/api/admin/reports/export/csv?type=SALES&preset=monthly")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/csv"))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("sales-report.csv")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("period")));
        }

        @Test
        @DisplayName("Excel export returns xlsx with attachment header")
        void excelExport() throws Exception {
            mockMvc.perform(get("/api/admin/reports/export/excel?type=INVENTORY")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inventory-report.xlsx")));
        }

        @Test
        @DisplayName("JSON export returns wrapped report")
        void jsonExport() throws Exception {
            mockMvc.perform(get("/api/admin/reports/export/json?type=PAYMENTS&preset=monthly")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.successfulPayments").isNumber());
        }
    }

    @Nested
    @DisplayName("Access control")
    class AccessControl {
        @Test
        @DisplayName("vendor token is forbidden on admin reports")
        void vendorForbidden() throws Exception {
            mockMvc.perform(get("/api/admin/reports/sales")
                            .header("Authorization", TestAuthTokens.vendor(tokenProvider, 2L)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("customer token is forbidden on admin reports")
        void customerForbidden() throws Exception {
            mockMvc.perform(get("/api/admin/reports/inventory")
                            .header("Authorization", TestAuthTokens.customer(tokenProvider, 3L)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("missing auth is unauthorized")
        void noAuth() throws Exception {
            mockMvc.perform(get("/api/admin/reports/sales"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
