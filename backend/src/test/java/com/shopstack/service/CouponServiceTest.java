package com.shopstack.service;

import com.shopstack.dto.coupon.CouponAnalyticsResponse;
import com.shopstack.dto.coupon.CouponApplyRequest;
import com.shopstack.dto.coupon.CouponCreateRequest;
import com.shopstack.dto.coupon.CouponResponse;
import com.shopstack.dto.coupon.CouponUpdateRequest;
import com.shopstack.dto.coupon.CouponUsageResponse;
import com.shopstack.dto.coupon.CouponValidateRequest;
import com.shopstack.dto.coupon.CouponValidateResponse;
import com.shopstack.entity.Coupon;
import com.shopstack.entity.CouponDiscountType;
import com.shopstack.entity.CouponUsage;
import com.shopstack.entity.Order;
import com.shopstack.entity.User;
import com.shopstack.exception.CouponAlreadyExistsException;
import com.shopstack.exception.CouponExpiredException;
import com.shopstack.exception.CouponInactiveException;
import com.shopstack.exception.CouponMinimumAmountException;
import com.shopstack.exception.CouponNotFoundException;
import com.shopstack.exception.CouponUsageExceededException;
import com.shopstack.exception.InvalidCouponException;
import com.shopstack.mapper.CouponMapper;
import com.shopstack.mapper.CouponUsageMapper;
import com.shopstack.repository.CouponRepository;
import com.shopstack.repository.CouponUsageRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private com.shopstack.repository.CartItemRepository cartItemRepository;

    @Mock
    private com.shopstack.repository.CategoryRepository categoryRepository;

    @Mock
    private com.shopstack.repository.ProductRepository productRepository;

    @Mock
    private com.shopstack.repository.VendorProfileRepository vendorProfileRepository;

    @Spy
    private CouponMapper couponMapper = new CouponMapper();

    @Spy
    private CouponUsageMapper couponUsageMapper = new CouponUsageMapper();

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon sampleCoupon;
    private Coupon fixedCoupon;
    private CouponCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        sampleCoupon = Coupon.builder()
                .id(1L)
                .code("SUMMER20")
                .name("Summer Sale")
                .discountType(CouponDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .minimumOrderAmount(new BigDecimal("100.00"))
                .maximumDiscount(new BigDecimal("50.00"))
                .usageLimit(100)
                .usedCount(0)
                .perUserLimit(1)
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(10))
                .active(true)
                .build();

        fixedCoupon = Coupon.builder()
                .id(2L)
                .code("FLAT500")
                .name("Flat 500 Off")
                .discountType(CouponDiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("500.00"))
                .minimumOrderAmount(new BigDecimal("1500.00"))
                .usageLimit(50)
                .usedCount(5)
                .perUserLimit(2)
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(10))
                .active(true)
                .build();

        createRequest = CouponCreateRequest.builder()
                .code("SUMMER20")
                .name("Summer Sale")
                .discountType(CouponDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .minimumOrderAmount(new BigDecimal("100.00"))
                .maximumDiscount(new BigDecimal("50.00"))
                .usageLimit(100)
                .perUserLimit(1)
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(10))
                .build();
    }

    @Nested
    @DisplayName("Create Coupon")
    class CreateCouponTests {

        @Test
        @DisplayName("Should successfully create a coupon")
        void shouldCreateCoupon() {
            when(couponRepository.existsByCodeIgnoreCase("SUMMER20")).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenReturn(sampleCoupon);

            CouponResponse response = couponService.createCoupon(createRequest);

            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo("SUMMER20");
            verify(couponRepository).save(any(Coupon.class));
        }

        @Test
        @DisplayName("Should throw exception when coupon code already exists")
        void shouldThrowExceptionWhenDuplicateCode() {
            when(couponRepository.existsByCodeIgnoreCase("SUMMER20")).thenReturn(true);

            assertThatThrownBy(() -> couponService.createCoupon(createRequest))
                    .isInstanceOf(CouponAlreadyExistsException.class)
                    .hasMessageContaining("SUMMER20");
        }

        @Test
        @DisplayName("Should throw exception when start date is after expiry date")
        void shouldThrowExceptionWhenInvalidDates() {
            createRequest.setStartDate(LocalDateTime.now().plusDays(5));
            createRequest.setExpiryDate(LocalDateTime.now().plusDays(1));

            assertThatThrownBy(() -> couponService.createCoupon(createRequest))
                    .isInstanceOf(InvalidCouponException.class);
        }
    }

    @Nested
    @DisplayName("Update and Status Management")
    class UpdateAndStatusTests {

        @Test
        @DisplayName("Should update coupon successfully")
        void shouldUpdateCoupon() {
            when(couponRepository.findById(1L)).thenReturn(Optional.of(sampleCoupon));
            when(couponRepository.save(any(Coupon.class))).thenReturn(sampleCoupon);

            CouponUpdateRequest updateRequest = CouponUpdateRequest.builder()
                    .name("Updated Sale")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("25.00"))
                    .startDate(LocalDateTime.now().minusDays(1))
                    .expiryDate(LocalDateTime.now().plusDays(20))
                    .build();

            CouponResponse response = couponService.updateCoupon(1L, updateRequest);
            assertThat(response).isNotNull();
            verify(couponRepository).save(any(Coupon.class));
        }

        @Test
        @DisplayName("Should enable/activate and disable/deactivate coupon")
        void shouldToggleCouponStatus() {
            when(couponRepository.findById(1L)).thenReturn(Optional.of(sampleCoupon));
            when(couponRepository.save(any(Coupon.class))).thenReturn(sampleCoupon);

            CouponResponse disabled = couponService.disableCoupon(1L);
            assertThat(disabled).isNotNull();

            CouponResponse enabled = couponService.enableCoupon(1L);
            assertThat(enabled).isNotNull();
        }

        @Test
        @DisplayName("Should delete coupon")
        void shouldDeleteCoupon() {
            when(couponRepository.existsById(1L)).thenReturn(true);

            couponService.deleteCoupon(1L);
            verify(couponRepository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Validate and Apply Coupon")
    class ValidateCouponTests {

        @Test
        @DisplayName("Should validate percentage coupon and calculate discount with max cap")
        void shouldValidatePercentageCouponWithCap() {
            when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(sampleCoupon));

            // Order 500: 20% is 100, but maxDiscount is 50. Discount should be 50. Final total 450.
            CouponValidateRequest request = CouponValidateRequest.builder()
                    .code("SUMMER20")
                    .orderTotal(new BigDecimal("500.00"))
                    .build();

            CouponValidateResponse response = couponService.validateCoupon(request);

            assertThat(response.isValid()).isTrue();
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(response.getFinalTotal()).isEqualByComparingTo(new BigDecimal("450.00"));
            assertThat(response.getMessage()).isEqualTo("Coupon applied successfully");
        }

        @Test
        @DisplayName("Should validate FIXED_AMOUNT coupon")
        void shouldValidateFixedAmountCoupon() {
            when(couponRepository.findByCodeIgnoreCase("FLAT500")).thenReturn(Optional.of(fixedCoupon));

            CouponValidateRequest request = CouponValidateRequest.builder()
                    .code("FLAT500")
                    .orderTotal(new BigDecimal("2000.00"))
                    .build();

            CouponValidateResponse response = couponService.validateCoupon(request);

            assertThat(response.isValid()).isTrue();
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(response.getFinalTotal()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("Should throw exception when minimum order amount not met")
        void shouldThrowExceptionWhenMinAmountNotMet() {
            when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(sampleCoupon));

            CouponValidateRequest request = CouponValidateRequest.builder()
                    .code("SUMMER20")
                    .orderTotal(new BigDecimal("50.00")) // Less than min 100
                    .build();

            assertThatThrownBy(() -> couponService.validateCoupon(request))
                    .isInstanceOf(CouponMinimumAmountException.class);
        }

        @Test
        @DisplayName("Should throw exception when coupon is inactive")
        void shouldThrowExceptionWhenInactive() {
            sampleCoupon.setActive(false);
            when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(sampleCoupon));

            CouponValidateRequest request = CouponValidateRequest.builder()
                    .code("SUMMER20")
                    .orderTotal(new BigDecimal("200.00"))
                    .build();

            assertThatThrownBy(() -> couponService.validateCoupon(request))
                    .isInstanceOf(CouponInactiveException.class);
        }

        @Test
        @DisplayName("Should throw exception when coupon is expired")
        void shouldThrowExceptionWhenExpired() {
            sampleCoupon.setExpiryDate(LocalDateTime.now().minusDays(1));
            when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(sampleCoupon));

            CouponValidateRequest request = CouponValidateRequest.builder()
                    .code("SUMMER20")
                    .orderTotal(new BigDecimal("200.00"))
                    .build();

            assertThatThrownBy(() -> couponService.validateCoupon(request))
                    .isInstanceOf(CouponExpiredException.class);
        }

        @Test
        @DisplayName("Should throw exception when coupon start date is in the future")
        void shouldThrowExceptionWhenFutureCoupon() {
            sampleCoupon.setStartDate(LocalDateTime.now().plusDays(2));
            when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(sampleCoupon));

            CouponValidateRequest request = CouponValidateRequest.builder()
                    .code("SUMMER20")
                    .orderTotal(new BigDecimal("200.00"))
                    .build();

            assertThatThrownBy(() -> couponService.validateCoupon(request))
                    .isInstanceOf(CouponInactiveException.class);
        }

        @Test
        @DisplayName("Should throw exception when per-user limit is reached")
        void shouldThrowExceptionWhenPerUserLimitReached() {
            User user = new User();
            user.setId(1L);
            user.setEmail("buyer@example.com");

            when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(sampleCoupon));
            when(userRepository.findByEmailIgnoreCase("buyer@example.com")).thenReturn(Optional.of(user));
            when(couponUsageRepository.countByCouponAndUser(sampleCoupon, user)).thenReturn(1); // Limit is 1

            CouponValidateRequest request = CouponValidateRequest.builder()
                    .code("SUMMER20")
                    .orderTotal(new BigDecimal("200.00"))
                    .userEmail("buyer@example.com")
                    .build();

            assertThatThrownBy(() -> couponService.validateCoupon(request))
                    .isInstanceOf(CouponUsageExceededException.class)
                    .hasMessageContaining("maximum usage limit");
        }
    }

    @Nested
    @DisplayName("Record Usage and Analytics")
    class UsageAndAnalyticsTests {

        @Test
        @DisplayName("Should record usage and increment count")
        void shouldRecordUsage() {
            User user = new User();
            user.setId(1L);
            user.setEmail("test@test.com");

            Order order = new Order();
            order.setId(10L);
            order.setTotalAmount(new BigDecimal("200.00"));

            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
            when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

            couponService.recordCouponUsage(sampleCoupon, 10L, "test@test.com");

            verify(couponUsageRepository).save(any());
            verify(couponRepository).save(sampleCoupon);
            assertThat(sampleCoupon.getUsedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get coupon usage history")
        void shouldGetCouponUsages() {
            when(couponRepository.existsById(1L)).thenReturn(true);

            User user = new User();
            user.setId(1L);
            user.setEmail("test@test.com");

            Order order = new Order();
            order.setId(10L);
            order.setOrderNumber("ORD-1001");

            CouponUsage usage = CouponUsage.builder()
                    .id(5L)
                    .coupon(sampleCoupon)
                    .user(user)
                    .order(order)
                    .discountAmount(new BigDecimal("40.00"))
                    .usedAt(LocalDateTime.now())
                    .build();

            when(couponUsageRepository.findByCouponIdOrderByUsedAtDesc(1L)).thenReturn(List.of(usage));

            List<CouponUsageResponse> usages = couponService.getCouponUsages(1L);
            assertThat(usages).hasSize(1);
            assertThat(usages.get(0).getCouponCode()).isEqualTo("SUMMER20");
            assertThat(usages.get(0).getOrderNumber()).isEqualTo("ORD-1001");
        }

        @Test
        @DisplayName("Should aggregate coupon analytics")
        void shouldGetCouponAnalytics() {
            when(couponRepository.findAll()).thenReturn(List.of(sampleCoupon, fixedCoupon));
            when(couponUsageRepository.countTotalUsages()).thenReturn(5L);
            when(couponUsageRepository.sumTotalDiscountAmount()).thenReturn(new BigDecimal("2500.00"));
            when(couponUsageRepository.sumDiscountAmountByCouponId(any())).thenReturn(new BigDecimal("500.00"));

            CouponAnalyticsResponse analytics = couponService.getCouponAnalytics();

            assertThat(analytics).isNotNull();
            assertThat(analytics.getTotalCoupons()).isEqualTo(2);
            assertThat(analytics.getTotalUsageCount()).isEqualTo(5);
            assertThat(analytics.getTotalDiscountGiven()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(analytics.getTopCouponCode()).isEqualTo("FLAT500");
            assertThat(analytics.getTopCouponUsageCount()).isEqualTo(5);
            assertThat(analytics.getCouponBreakdown()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Coupon Applicability Scope Tests")
    class ApplicabilityScopeTests {

        @Test
        @DisplayName("Should create SPECIFIC_CATEGORIES scoped coupon when valid category IDs provided")
        void shouldCreateCategoryScopedCoupon() {
            com.shopstack.entity.Category cat = com.shopstack.entity.Category.builder().id(10L).name("Electronics").slug("electronics").build();
            when(couponRepository.existsByCodeIgnoreCase("TECH20")).thenReturn(false);
            when(categoryRepository.findAllById(any())).thenReturn(List.of(cat));

            Coupon savedCategoryCoupon = Coupon.builder()
                    .id(3L)
                    .code("TECH20")
                    .name("Tech 20% Off")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20.00"))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES)
                    .eligibleCategories(java.util.Set.of(cat))
                    .active(true)
                    .build();
            when(couponRepository.save(any(Coupon.class))).thenReturn(savedCategoryCoupon);

            CouponCreateRequest request = CouponCreateRequest.builder()
                    .code("TECH20")
                    .name("Tech 20% Off")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20.00"))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES)
                    .categoryIds(java.util.Set.of(10L))
                    .build();

            CouponResponse response = couponService.createCoupon(request);

            assertThat(response).isNotNull();
            assertThat(response.getApplicabilityScope()).isEqualTo(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES);
            assertThat(response.getEligibleCategoryIds()).contains(10L);
        }

        @Test
        @DisplayName("Should throw error if SPECIFIC_CATEGORIES scoped coupon has no categories")
        void shouldThrowWhenCategoryScopeHasNoCategories() {
            CouponCreateRequest request = CouponCreateRequest.builder()
                    .code("TECH20")
                    .name("Tech 20% Off")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20.00"))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES)
                    .categoryIds(java.util.Set.of())
                    .build();

            assertThatThrownBy(() -> couponService.createCoupon(request))
                    .isInstanceOf(InvalidCouponException.class)
                    .hasMessageContaining("At least one eligible category is required");
        }

        @Test
        @DisplayName("Should return only applicable coupons matching cart items in getApplicableCoupons")
        void shouldReturnOnlyApplicableCouponsForCart() {
            User user = new User();
            user.setId(1L);
            user.setEmail("buyer@shopstack.com");

            com.shopstack.entity.Category electronics = com.shopstack.entity.Category.builder().id(10L).name("Electronics").build();
            com.shopstack.entity.Category fashion = com.shopstack.entity.Category.builder().id(20L).name("Fashion").build();

            com.shopstack.entity.Product speaker = com.shopstack.entity.Product.builder().id(101L).name("Speaker").category(electronics).price(new BigDecimal("1800.00")).build();
            com.shopstack.entity.CartItem cartItem = com.shopstack.entity.CartItem.builder().id(1L).user(user).product(speaker).quantity(1).build();

            Coupon electronicsCoupon = Coupon.builder()
                    .id(11L)
                    .code("ELEC20")
                    .name("20% Off Electronics")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20.00"))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES)
                    .eligibleCategories(java.util.Set.of(electronics))
                    .active(true)
                    .build();

            Coupon fashionCoupon = Coupon.builder()
                    .id(12L)
                    .code("FASHION30")
                    .name("30% Off Fashion")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("30.00"))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES)
                    .eligibleCategories(java.util.Set.of(fashion))
                    .active(true)
                    .build();

            when(userRepository.findByEmailIgnoreCase("buyer@shopstack.com")).thenReturn(Optional.of(user));
            when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem));
            when(couponRepository.findActiveCoupons(any())).thenReturn(List.of(electronicsCoupon, fashionCoupon));

            List<com.shopstack.dto.coupon.ApplicableCouponResponse> results = couponService.getApplicableCoupons("buyer@shopstack.com");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCode()).isEqualTo("ELEC20");
            assertThat(results.get(0).getEligibleSubtotal()).isEqualByComparingTo(new BigDecimal("1800.00"));
            assertThat(results.get(0).getEstimatedDiscount()).isEqualByComparingTo(new BigDecimal("360.00"));
        }

        @Test
        @DisplayName("Should reject ineligible category coupon when manually validated against cart")
        void shouldRejectIneligibleCouponOnCartValidation() {
            User user = new User();
            user.setId(1L);
            user.setEmail("buyer@shopstack.com");

            com.shopstack.entity.Category fashion = com.shopstack.entity.Category.builder().id(20L).name("Fashion").build();
            com.shopstack.entity.Category electronics = com.shopstack.entity.Category.builder().id(10L).name("Electronics").build();

            com.shopstack.entity.Product speaker = com.shopstack.entity.Product.builder().id(101L).name("Speaker").category(electronics).price(new BigDecimal("1800.00")).build();
            com.shopstack.entity.CartItem cartItem = com.shopstack.entity.CartItem.builder().id(1L).user(user).product(speaker).quantity(1).build();

            Coupon fashionCoupon = Coupon.builder()
                    .id(12L)
                    .code("FASHION30")
                    .name("30% Off Fashion")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("30.00"))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES)
                    .eligibleCategories(java.util.Set.of(fashion))
                    .active(true)
                    .build();

            when(couponRepository.findByCodeIgnoreCase("FASHION30")).thenReturn(Optional.of(fashionCoupon));
            when(userRepository.findByEmailIgnoreCase("buyer@shopstack.com")).thenReturn(Optional.of(user));
            when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem));

            CouponValidateRequest req = CouponValidateRequest.builder()
                    .code("FASHION30")
                    .orderTotal(new BigDecimal("1800.00"))
                    .userEmail("buyer@shopstack.com")
                    .build();

            assertThatThrownBy(() -> couponService.validateCoupon(req))
                    .isInstanceOf(InvalidCouponException.class)
                    .hasMessage("This coupon is not applicable to the products in your cart.");
        }

        @Test
        @DisplayName("Should calculate mixed cart discount strictly on eligible items")
        void shouldCalculateDiscountOnEligibleItemsInMixedCart() {
            User user = new User();
            user.setId(1L);
            user.setEmail("buyer@shopstack.com");

            com.shopstack.entity.Category electronics = com.shopstack.entity.Category.builder().id(10L).name("Electronics").build();
            com.shopstack.entity.Category fashion = com.shopstack.entity.Category.builder().id(20L).name("Fashion").build();

            com.shopstack.entity.Product speaker = com.shopstack.entity.Product.builder().id(101L).name("Speaker").category(electronics).price(new BigDecimal("1800.00")).build();
            com.shopstack.entity.Product shirt = com.shopstack.entity.Product.builder().id(102L).name("Shirt").category(fashion).price(new BigDecimal("1000.00")).build();

            com.shopstack.entity.CartItem item1 = com.shopstack.entity.CartItem.builder().id(1L).user(user).product(speaker).quantity(1).build();
            com.shopstack.entity.CartItem item2 = com.shopstack.entity.CartItem.builder().id(2L).user(user).product(shirt).quantity(1).build();

            Coupon electronicsCoupon = Coupon.builder()
                    .id(11L)
                    .code("ELEC20")
                    .name("20% Off Electronics")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20.00"))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES)
                    .eligibleCategories(java.util.Set.of(electronics))
                    .active(true)
                    .build();

            when(couponRepository.findByCodeIgnoreCase("ELEC20")).thenReturn(Optional.of(electronicsCoupon));
            when(userRepository.findByEmailIgnoreCase("buyer@shopstack.com")).thenReturn(Optional.of(user));
            when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(item1, item2));

            CouponValidateRequest req = CouponValidateRequest.builder()
                    .code("ELEC20")
                    .orderTotal(new BigDecimal("2800.00"))
                    .userEmail("buyer@shopstack.com")
                    .build();

            CouponValidateResponse resp = couponService.validateCoupon(req);

            assertThat(resp.isValid()).isTrue();
            assertThat(resp.getEligibleSubtotal()).isEqualByComparingTo(new BigDecimal("1800.00"));
            // 20% of 1800 = 360
            assertThat(resp.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("360.00"));
            // 2800 - 360 = 2440
            assertThat(resp.getFinalTotal()).isEqualByComparingTo(new BigDecimal("2440.00"));
        }

        @Test
        @DisplayName("Should create PRODUCT scoped coupon and validate strictly on target product")
        void shouldHandleProductScopedCoupon() {
            com.shopstack.entity.Product product1 = com.shopstack.entity.Product.builder().id(101L).name("Smartphone").price(new BigDecimal("10000.00")).build();
            com.shopstack.entity.Product product2 = com.shopstack.entity.Product.builder().id(102L).name("Case").price(new BigDecimal("500.00")).build();

            User user = new User();
            user.setId(1L);
            user.setEmail("buyer@shopstack.com");

            com.shopstack.entity.CartItem item1 = com.shopstack.entity.CartItem.builder().id(1L).user(user).product(product1).quantity(1).build();
            com.shopstack.entity.CartItem item2 = com.shopstack.entity.CartItem.builder().id(2L).user(user).product(product2).quantity(1).build();

            Coupon productCoupon = Coupon.builder()
                    .id(20L)
                    .code("PHONE1000")
                    .name("Flat 1000 Off Phone")
                    .discountType(CouponDiscountType.FIXED_AMOUNT)
                    .discountValue(new BigDecimal("1000.00"))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_PRODUCTS)
                    .eligibleProducts(java.util.Set.of(product1))
                    .active(true)
                    .build();

            when(couponRepository.findByCodeIgnoreCase("PHONE1000")).thenReturn(Optional.of(productCoupon));
            when(userRepository.findByEmailIgnoreCase("buyer@shopstack.com")).thenReturn(Optional.of(user));
            when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(item1, item2));

            CouponValidateRequest req = CouponValidateRequest.builder()
                    .code("PHONE1000")
                    .orderTotal(new BigDecimal("10500.00"))
                    .userEmail("buyer@shopstack.com")
                    .build();

            CouponValidateResponse resp = couponService.validateCoupon(req);
            assertThat(resp.isValid()).isTrue();
            assertThat(resp.getEligibleSubtotal()).isEqualByComparingTo(new BigDecimal("10000.00"));
            assertThat(resp.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(resp.getFinalTotal()).isEqualByComparingTo(new BigDecimal("9500.00"));
        }

        @Test
        @DisplayName("Should create VENDOR scoped coupon and validate strictly on vendor products")
        void shouldHandleVendorScopedCoupon() {
            com.shopstack.entity.VendorProfile vendorA = com.shopstack.entity.VendorProfile.builder().id(5L).storeName("Acme Store").build();
            com.shopstack.entity.VendorProfile vendorB = com.shopstack.entity.VendorProfile.builder().id(6L).storeName("Beta Store").build();

            com.shopstack.entity.Product productA = com.shopstack.entity.Product.builder().id(201L).name("Acme Tool").vendorProfile(vendorA).price(new BigDecimal("2000.00")).build();
            com.shopstack.entity.Product productB = com.shopstack.entity.Product.builder().id(202L).name("Beta Widget").vendorProfile(vendorB).price(new BigDecimal("1000.00")).build();

            User user = new User();
            user.setId(1L);
            user.setEmail("buyer@shopstack.com");

            com.shopstack.entity.CartItem itemA = com.shopstack.entity.CartItem.builder().id(1L).user(user).product(productA).quantity(1).build();
            com.shopstack.entity.CartItem itemB = com.shopstack.entity.CartItem.builder().id(2L).user(user).product(productB).quantity(1).build();

            Coupon vendorCoupon = Coupon.builder()
                    .id(30L)
                    .code("ACME15")
                    .name("15% Off Acme")
                    .discountType(CouponDiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("15.00"))
                    .applicabilityScope(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_VENDORS)
                    .eligibleVendors(java.util.Set.of(vendorA))
                    .active(true)
                    .build();

            when(couponRepository.findByCodeIgnoreCase("ACME15")).thenReturn(Optional.of(vendorCoupon));
            when(userRepository.findByEmailIgnoreCase("buyer@shopstack.com")).thenReturn(Optional.of(user));
            when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(itemA, itemB));

            CouponValidateRequest req = CouponValidateRequest.builder()
                    .code("ACME15")
                    .orderTotal(new BigDecimal("3000.00"))
                    .userEmail("buyer@shopstack.com")
                    .build();

            CouponValidateResponse resp = couponService.validateCoupon(req);
            assertThat(resp.isValid()).isTrue();
            assertThat(resp.getEligibleSubtotal()).isEqualByComparingTo(new BigDecimal("2000.00"));
            // 15% of 2000 = 300
            assertThat(resp.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
            // 3000 - 300 = 2700
            assertThat(resp.getFinalTotal()).isEqualByComparingTo(new BigDecimal("2700.00"));
        }

        @Test
        @DisplayName("Should correctly deserialize canonical and alias JSON values for CouponApplicabilityScope")
        void shouldDeserializeCanonicalAndAliasJsonValues() throws Exception {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            // Canonical values
            assertThat(mapper.readValue("\"ENTIRE_PLATFORM\"", com.shopstack.entity.CouponApplicabilityScope.class))
                    .isEqualTo(com.shopstack.entity.CouponApplicabilityScope.ENTIRE_PLATFORM);
            assertThat(mapper.readValue("\"SPECIFIC_CATEGORIES\"", com.shopstack.entity.CouponApplicabilityScope.class))
                    .isEqualTo(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES);
            assertThat(mapper.readValue("\"SPECIFIC_PRODUCTS\"", com.shopstack.entity.CouponApplicabilityScope.class))
                    .isEqualTo(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_PRODUCTS);
            assertThat(mapper.readValue("\"SPECIFIC_VENDORS\"", com.shopstack.entity.CouponApplicabilityScope.class))
                    .isEqualTo(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_VENDORS);

            // Backward compatible aliases
            assertThat(mapper.readValue("\"PLATFORM\"", com.shopstack.entity.CouponApplicabilityScope.class))
                    .isEqualTo(com.shopstack.entity.CouponApplicabilityScope.ENTIRE_PLATFORM);
            assertThat(mapper.readValue("\"CATEGORY\"", com.shopstack.entity.CouponApplicabilityScope.class))
                    .isEqualTo(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_CATEGORIES);
            assertThat(mapper.readValue("\"PRODUCT\"", com.shopstack.entity.CouponApplicabilityScope.class))
                    .isEqualTo(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_PRODUCTS);
            assertThat(mapper.readValue("\"VENDOR\"", com.shopstack.entity.CouponApplicabilityScope.class))
                    .isEqualTo(com.shopstack.entity.CouponApplicabilityScope.SPECIFIC_VENDORS);

            // Case-insensitivity and whitespace
            assertThat(mapper.readValue("\"  entire_platform  \"", com.shopstack.entity.CouponApplicabilityScope.class))
                    .isEqualTo(com.shopstack.entity.CouponApplicabilityScope.ENTIRE_PLATFORM);

            // Full request JSON deserialization
            String json = "{\"code\":\"TEST\",\"name\":\"Test Coupon\",\"discountType\":\"PERCENTAGE\",\"discountValue\":10,\"applicabilityScope\":\"ENTIRE_PLATFORM\"}";
            CouponCreateRequest parsed = mapper.readValue(json, CouponCreateRequest.class);
            assertThat(parsed.getApplicabilityScope()).isEqualTo(com.shopstack.entity.CouponApplicabilityScope.ENTIRE_PLATFORM);

            // Invalid value throws IllegalArgumentException
            assertThatThrownBy(() -> mapper.readValue("\"INVALID_SCOPE\"", com.shopstack.entity.CouponApplicabilityScope.class))
                    .isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException.class);
        }
    }
}



