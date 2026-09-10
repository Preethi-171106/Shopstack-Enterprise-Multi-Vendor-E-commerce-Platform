package com.shopstack.service;

import com.shopstack.dto.order.OrderCreateRequest;
import com.shopstack.dto.order.OrderResponse;
import com.shopstack.entity.CartItem;
import com.shopstack.entity.Coupon;
import com.shopstack.entity.CouponDiscountType;
import com.shopstack.entity.Order;
import com.shopstack.entity.Product;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.exception.CouponMinimumAmountException;
import com.shopstack.exception.InvalidCouponException;
import com.shopstack.mapper.OrderMapper;
import com.shopstack.repository.CartItemRepository;
import com.shopstack.repository.CouponRepository;
import com.shopstack.repository.CouponUsageRepository;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private CommissionService commissionService;

    @Mock
    private NotificationService notificationService;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @InjectMocks
    private OrderService orderService;

    private User customer;
    private Product product;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        customer = User.builder().id(1L).email("customer@shopstack.com").role(UserRole.CUSTOMER).build();

        VendorProfile vendorProfile = VendorProfile.builder().id(100L).storeName("Northwind").build();
        product = Product.builder()
                .id(42L)
                .name("USB-C Charger")
                .slug("usb-c-charger")
                .price(new BigDecimal("299.00"))
                .stockQuantity(10)
                .active(true)
                .vendorProfile(vendorProfile)
                .imageUrl("https://example.com/charger.jpg")
                .build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                )
        );
    }

    @Test
    @DisplayName("Should create order preserving exact quantity of 1 for a single product")
    void shouldCreateOrderWithExactQuantityOne() {
        authenticateAs(customer);
        CartItem cartItem = CartItem.builder().id(11L).user(customer).product(product).quantity(1).build();

        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdForUpdate(1L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.existsByProductId(42L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(901L);
            return o;
        });

        OrderResponse response = orderService.createOrder(OrderCreateRequest.builder()
                .shippingAddress("123 Main Street")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(1);
        assertThat(response.getItems().get(0).getPrice()).isEqualByComparingTo("299.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("299.00");
        assertThat(product.getStockQuantity()).isEqualTo(9); // 10 - 1

        org.mockito.Mockito.verify(cartItemRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("Should create order preserving exact quantity of 2 for a single product")
    void shouldCreateOrderWithExactQuantityTwo() {
        authenticateAs(customer);
        CartItem cartItem = CartItem.builder().id(11L).user(customer).product(product).quantity(2).build();

        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdForUpdate(1L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.existsByProductId(42L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(902L);
            return o;
        });

        OrderResponse response = orderService.createOrder(OrderCreateRequest.builder()
                .shippingAddress("123 Main Street")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("598.00");
        assertThat(product.getStockQuantity()).isEqualTo(8); // 10 - 2

        org.mockito.Mockito.verify(cartItemRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("Should create order preserving exact quantity of 5 for a single product")
    void shouldCreateOrderWithExactQuantityFive() {
        authenticateAs(customer);
        CartItem cartItem = CartItem.builder().id(11L).user(customer).product(product).quantity(5).build();

        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdForUpdate(1L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.existsByProductId(42L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(905L);
            return o;
        });

        OrderResponse response = orderService.createOrder(OrderCreateRequest.builder()
                .shippingAddress("123 Main Street")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("1495.00");
        assertThat(product.getStockQuantity()).isEqualTo(5); // 10 - 5

        org.mockito.Mockito.verify(cartItemRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("Should create order with multiple distinct products preserving exact quantities")
    void shouldCreateOrderWithMultipleDistinctProducts() {
        authenticateAs(customer);
        Product product2 = Product.builder()
                .id(43L)
                .name("Wireless Mouse")
                .slug("wireless-mouse")
                .price(new BigDecimal("49.00"))
                .stockQuantity(20)
                .active(true)
                .vendorProfile(product.getVendorProfile())
                .imageUrl("https://example.com/mouse.jpg")
                .build();

        CartItem item1 = CartItem.builder().id(11L).user(customer).product(product).quantity(2).build();
        CartItem item2 = CartItem.builder().id(12L).user(customer).product(product2).quantity(3).build();

        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdForUpdate(1L)).thenReturn(List.of(item1, item2));
        when(inventoryRepository.existsByProductId(42L)).thenReturn(false);
        when(inventoryRepository.existsByProductId(43L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(910L);
            return o;
        });

        OrderResponse response = orderService.createOrder(OrderCreateRequest.builder()
                .shippingAddress("123 Main Street")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getItems().get(1).getQuantity()).isEqualTo(3);
        // Total = 2*299.00 + 3*49.00 = 598.00 + 147.00 = 745.00
        assertThat(response.getTotalAmount()).isEqualByComparingTo("745.00");
    }

    @Test
    @DisplayName("Should prevent duplicate order creation when cart is empty or already checked out")
    void shouldPreventDuplicateOrderCreationOnEmptyCart() {
        authenticateAs(customer);
        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdForUpdate(1L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> orderService.createOrder(OrderCreateRequest.builder()
                .shippingAddress("123 Main Street")
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty cart");

        org.mockito.Mockito.verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should reject invalid coupons during order creation")
    void shouldRejectInvalidCouponCodeDuringOrderCreation() {
        authenticateAs(customer);
        CartItem cartItem = CartItem.builder().id(11L).user(customer).product(product).quantity(1).build();

        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdForUpdate(1L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.existsByProductId(42L)).thenReturn(false);
        when(couponRepository.findByCodeIgnoreCase("BADCODE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(OrderCreateRequest.builder()
                .shippingAddress("123 Main Street")
                .couponCode("BADCODE")
                .build()))
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("Coupon not found");

        org.mockito.Mockito.verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should reject coupons below the minimum order amount before placing an order")
    void shouldRejectCouponBelowMinimumOrderAmount() {
        authenticateAs(customer);
        CartItem cartItem = CartItem.builder().id(12L).user(customer).product(product).quantity(1).build();
        Coupon coupon = Coupon.builder()
                .id(5L)
                .code("SAVE10")
                .name("Save 10")
                .discountType(CouponDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .minimumOrderAmount(new BigDecimal("500.00"))
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(5))
                .active(true)
                .usedCount(0)
                .build();

        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdForUpdate(1L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.existsByProductId(42L)).thenReturn(false);
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> orderService.createOrder(OrderCreateRequest.builder()
                .shippingAddress("123 Main Street")
                .couponCode("SAVE10")
                .build()))
                .isInstanceOf(CouponMinimumAmountException.class)
                .hasMessageContaining("minimum required amount");

        org.mockito.Mockito.verify(orderRepository, never()).save(any(Order.class));
    }
}
