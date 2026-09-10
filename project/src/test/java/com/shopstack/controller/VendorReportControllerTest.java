package com.shopstack.controller;

import com.shopstack.entity.*;
import com.shopstack.repository.*;
import com.shopstack.security.jwt.JwtTokenProvider;
import com.shopstack.support.TestAuthTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@DisplayName("VendorReportController integration tests")
class VendorReportControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired UserRepository userRepository;
    @Autowired VendorRepository vendorRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;

    private Long vendorUserId;

    @BeforeEach
    void seed() {
        userRepository.save(User.builder()
                .email("admin@shopstack.com").password("x").fullName("Admin").role(UserRole.ADMIN).build());
        User vendorUser = userRepository.save(User.builder()
                .email("vendor@shopstack.com").password("x").fullName("Vendor").role(UserRole.VENDOR).build());
        vendorUserId = vendorUser.getId();
        Vendor own = vendorRepository.save(Vendor.builder().name("MyStore").userId(vendorUser.getId()).build());
        Vendor other = vendorRepository.save(Vendor.builder().name("OtherStore").userId(999L).build());
        Category cat = categoryRepository.save(Category.builder().name("Books").build());

        productRepository.save(Product.builder()
                .name("MyProduct").price(new BigDecimal("20.00")).stockQuantity(1).lowStockThreshold(5)
                .vendor(own).category(cat).build());
        productRepository.save(Product.builder()
                .name("OtherProduct").price(new BigDecimal("20.00")).stockQuantity(100).lowStockThreshold(5)
                .vendor(other).category(cat).build());

        orderRepository.save(Order.builder()
                .userId(3L).vendorId(own.getId()).orderDate(LocalDateTime.now())
                .status(OrderStatus.PAID).totalAmount(new BigDecimal("80.00"))
                .discountAmount(BigDecimal.ZERO).build());
        orderRepository.save(Order.builder()
                .userId(3L).vendorId(other.getId()).orderDate(LocalDateTime.now())
                .status(OrderStatus.PAID).totalAmount(new BigDecimal("999.00"))
                .discountAmount(BigDecimal.ZERO).build());
    }

    private String vendorToken() {
        return TestAuthTokens.vendor(tokenProvider, vendorUserId);
    }

    @Nested
    @DisplayName("Vendor self reports")
    class Self {
        @Test
        @DisplayName("GET /api/vendor/reports returns the vendor's own report only")
        void overview() throws Exception {
            mockMvc.perform(get("/api/vendor/reports?preset=monthly")
                            .header("Authorization", vendorToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.vendors").isArray());
        }

        @Test
        @DisplayName("GET /api/vendor/reports/sales returns the vendor's own sales")
        void sales() throws Exception {
            mockMvc.perform(get("/api/vendor/reports/sales?preset=monthly")
                            .header("Authorization", vendorToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.revenue").exists());
        }

        @Test
        @DisplayName("GET /api/vendor/reports/products returns the vendor's own products")
        void products() throws Exception {
            mockMvc.perform(get("/api/vendor/reports/products?preset=monthly")
                            .header("Authorization", vendorToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lowStockProducts").isArray());
        }

        @Test
        @DisplayName("GET /api/vendor/reports/inventory returns the vendor's own inventory")
        void inventory() throws Exception {
            mockMvc.perform(get("/api/vendor/reports/inventory")
                            .header("Authorization", vendorToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalProducts").isNumber());
        }
    }

    @Nested
    @DisplayName("Vendor exports")
    class Exports {
        @Test
        @DisplayName("CSV export works for vendor's own sales")
        void csv() throws Exception {
            mockMvc.perform(get("/api/vendor/reports/export/csv?type=SALES&preset=monthly")
                            .header("Authorization", vendorToken()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/csv"))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("vendor-sales-report.csv")));
        }

        @Test
        @DisplayName("Excel export works for vendor's own products")
        void excel() throws Exception {
            mockMvc.perform(get("/api/vendor/reports/export/excel?type=PRODUCTS&preset=monthly")
                            .header("Authorization", vendorToken()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }
    }

    @Nested
    @DisplayName("Access control")
    class AccessControl {
        @Test
        @DisplayName("admin cannot use vendor endpoints")
        void adminForbiddenOnVendor() throws Exception {
            mockMvc.perform(get("/api/vendor/reports")
                            .header("Authorization", TestAuthTokens.admin(tokenProvider, 1L)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("customer cannot use vendor endpoints")
        void customerForbidden() throws Exception {
            mockMvc.perform(get("/api/vendor/reports")
                            .header("Authorization", TestAuthTokens.customer(tokenProvider, 3L)))
                    .andExpect(status().isForbidden());
        }
    }
}
