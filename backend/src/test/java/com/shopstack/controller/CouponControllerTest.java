package com.shopstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstack.dto.coupon.CouponAnalyticsResponse;
import com.shopstack.dto.coupon.CouponApplyRequest;
import com.shopstack.dto.coupon.CouponCreateRequest;
import com.shopstack.dto.coupon.CouponResponse;
import com.shopstack.dto.coupon.CouponUsageResponse;
import com.shopstack.dto.coupon.CouponValidateRequest;
import com.shopstack.dto.coupon.CouponValidateResponse;
import com.shopstack.entity.CouponDiscountType;
import com.shopstack.exception.CouponAlreadyExistsException;
import com.shopstack.exception.CouponNotFoundException;
import com.shopstack.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CouponService couponService;

    private CouponResponse sampleResponse;
    private CouponValidateResponse sampleValidateResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = CouponResponse.builder()
                .id(1L)
                .code("SUMMER20")
                .name("Summer Sale")
                .discountType(CouponDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .active(true)
                .status("ACTIVE")
                .build();

        sampleValidateResponse = CouponValidateResponse.builder()
                .valid(true)
                .code("SUMMER20")
                .name("Summer Sale")
                .discountType(CouponDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .discountAmount(new BigDecimal("40.00"))
                .finalTotal(new BigDecimal("160.00"))
                .message("Coupon applied successfully")
                .build();
    }

    @Nested
    @DisplayName("Admin Coupon APIs")
    class AdminCouponTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create coupon with 201 for ADMIN")
        void shouldCreateCouponForAdmin() throws Exception {
            given(couponService.createCoupon(any(CouponCreateRequest.class))).willReturn(sampleResponse);

            CouponCreateRequest request = CouponCreateRequest.builder()
                    .code("SUMMER20")
                    .name("Summer Sale")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20.00"))
                    .startDate(LocalDateTime.now())
                    .expiryDate(LocalDateTime.now().plusDays(10))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.ENTIRE_PLATFORM)
                    .build();

            mockMvc.perform(post("/api/admin/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUMMER20"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should successfully create coupon with raw JSON containing ENTIRE_PLATFORM and alias PLATFORM")
        void shouldCreateCouponWithEntirePlatformAndAlias() throws Exception {
            given(couponService.createCoupon(any(CouponCreateRequest.class))).willReturn(sampleResponse);

            String jsonEntirePlatform = """
                {
                    "code": "CANONICAL20",
                    "name": "Canonical Test",
                    "discountType": "PERCENTAGE",
                    "discountValue": 20.00,
                    "applicabilityScope": "ENTIRE_PLATFORM",
                    "startDate": "2026-08-27T10:00:00",
                    "expiryDate": "2026-09-27T10:00:00"
                }
                """;

            mockMvc.perform(post("/api/admin/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonEntirePlatform))
                    .andExpect(status().isCreated());

            String jsonAliasPlatform = """
                {
                    "code": "ALIAS20",
                    "name": "Alias Test",
                    "discountType": "PERCENTAGE",
                    "discountValue": 20.00,
                    "applicabilityScope": "PLATFORM",
                    "startDate": "2026-08-27T10:00:00",
                    "expiryDate": "2026-09-27T10:00:00"
                }
                """;

            mockMvc.perform(post("/api/admin/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonAliasPlatform))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 403 for CUSTOMER creating coupon")
        void shouldForbidCustomerFromCreating() throws Exception {
            CouponCreateRequest request = CouponCreateRequest.builder()
                    .code("SUMMER20")
                    .name("Summer Sale")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20.00"))
                    .startDate(LocalDateTime.now())
                    .expiryDate(LocalDateTime.now().plusDays(10))
                    .build();

            mockMvc.perform(post("/api/admin/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "VENDOR")
        @DisplayName("Should return 403 for VENDOR accessing admin coupons")
        void shouldForbidVendorFromAdmin() throws Exception {
            mockMvc.perform(get("/api/admin/coupons"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        @DisplayName("Should return 403 for WAREHOUSE_STAFF accessing admin coupons")
        void shouldForbidWarehouseStaffFromAdmin() throws Exception {
            mockMvc.perform(get("/api/admin/coupons"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated request on admin coupons")
        void shouldReturn401ForUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/admin/coupons"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should activate and deactivate coupon for ADMIN")
        void shouldActivateAndDeactivateCoupon() throws Exception {
            given(couponService.enableCoupon(1L)).willReturn(sampleResponse);
            given(couponService.disableCoupon(1L)).willReturn(sampleResponse);

            mockMvc.perform(patch("/api/admin/coupons/1/activate"))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/api/admin/coupons/1/deactivate"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete coupon for ADMIN")
        void shouldDeleteCoupon() throws Exception {
            mockMvc.perform(delete("/api/admin/coupons/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should fetch coupon analytics for ADMIN")
        void shouldGetAnalytics() throws Exception {
            CouponAnalyticsResponse analytics = CouponAnalyticsResponse.builder()
                    .totalCoupons(5)
                    .activeCoupons(4)
                    .totalUsageCount(25)
                    .totalDiscountGiven(new BigDecimal("1250.00"))
                    .topCouponCode("SAVE10")
                    .build();

            given(couponService.getCouponAnalytics()).willReturn(analytics);

            mockMvc.perform(get("/api/admin/coupons/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCoupons").value(5))
                    .andExpect(jsonPath("$.topCouponCode").value("SAVE10"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should fetch coupon usages for ADMIN")
        void shouldGetUsages() throws Exception {
            CouponUsageResponse usage = CouponUsageResponse.builder()
                    .id(1L)
                    .couponId(1L)
                    .couponCode("SUMMER20")
                    .userEmail("cust@test.com")
                    .discountAmount(new BigDecimal("40.00"))
                    .build();

            given(couponService.getCouponUsages(1L)).willReturn(List.of(usage));

            mockMvc.perform(get("/api/admin/coupons/1/usage"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].couponCode").value("SUMMER20"));
        }
    }

    @Nested
    @DisplayName("Customer Coupon APIs")
    class CustomerCouponTests {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should successfully validate coupon via POST /api/coupons/validate")
        void shouldValidateCoupon() throws Exception {
            given(couponService.validateCoupon(any(CouponValidateRequest.class))).willReturn(sampleValidateResponse);

            CouponValidateRequest request = CouponValidateRequest.builder()
                    .code("SUMMER20")
                    .orderTotal(new BigDecimal("200.00"))
                    .build();

            mockMvc.perform(post("/api/coupons/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.code").value("SUMMER20"))
                    .andExpect(jsonPath("$.discountAmount").value(40.00))
                    .andExpect(jsonPath("$.finalTotal").value(160.00));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should successfully apply coupon")
        void shouldApplyCoupon() throws Exception {
            given(couponService.applyCoupon(any(CouponApplyRequest.class))).willReturn(sampleResponse);

            CouponApplyRequest request = CouponApplyRequest.builder()
                    .code("SUMMER20")
                    .orderTotal(new BigDecimal("200.00"))
                    .build();

            mockMvc.perform(post("/api/coupons/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUMMER20"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 404 when applying non-existent coupon")
        void shouldReturn404WhenCouponNotFound() throws Exception {
            given(couponService.applyCoupon(any(CouponApplyRequest.class)))
                    .willThrow(new CouponNotFoundException("Not found"));

            CouponApplyRequest request = CouponApplyRequest.builder()
                    .code("INVALID")
                    .orderTotal(new BigDecimal("200.00"))
                    .build();

            mockMvc.perform(post("/api/coupons/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "VENDOR")
        @DisplayName("Should return 403 for VENDOR applying coupon")
        void shouldForbidVendorFromApplying() throws Exception {
            CouponApplyRequest request = CouponApplyRequest.builder()
                    .code("SUMMER20")
                    .orderTotal(new BigDecimal("200.00"))
                    .build();

            mockMvc.perform(post("/api/coupons/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should list available coupons")
        void shouldListAvailableCoupons() throws Exception {
            given(couponService.listAvailableCoupons()).willReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/coupons/available"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].code").value("SUMMER20"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return applicable coupons for customer cart")
        void shouldGetApplicableCoupons() throws Exception {
            com.shopstack.dto.coupon.ApplicableCouponResponse appResp = com.shopstack.dto.coupon.ApplicableCouponResponse.builder()
                    .id(1L)
                    .code("TECH20")
                    .name("Tech 20% Off")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20.00"))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES)
                    .eligibleSubtotal(new BigDecimal("1800.00"))
                    .estimatedDiscount(new BigDecimal("360.00"))
                    .message("20% OFF on eligible category items")
                    .build();

            given(couponService.getApplicableCoupons()).willReturn(List.of(appResp));

            mockMvc.perform(get("/api/coupons/applicable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].code").value("TECH20"))
                    .andExpect(jsonPath("$[0].applicabilityScope").value("SPECIFIC_CATEGORIES"))
                    .andExpect(jsonPath("$[0].eligibleSubtotal").value(1800.00))
                    .andExpect(jsonPath("$[0].estimatedDiscount").value(360.00));
        }
    }
}

