package com.shopstack.service;

import com.shopstack.dto.cart.CartItemRequest;
import com.shopstack.dto.cart.CartItemUpdateRequest;
import com.shopstack.dto.cart.CartResponse;
import com.shopstack.entity.CartItem;
import com.shopstack.entity.Product;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.exception.CartItemNotFoundException;
import com.shopstack.exception.InsufficientStockException;
import com.shopstack.exception.ProductNotFoundException;
import com.shopstack.mapper.CartMapper;
import com.shopstack.repository.CartItemRepository;
import com.shopstack.repository.ProductRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private CartMapper cartMapper = new CartMapper();

    @InjectMocks
    private CartService cartService;

    private User customer;
    private Product product;
    private VendorProfile vendorProfile;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        customer = User.builder()
                .id(1L)
                .email("customer@shopstack.com")
                .role(UserRole.CUSTOMER)
                .build();

        vendorProfile = VendorProfile.builder()
                .id(7L)
                .storeName("Northwind Stores")
                .build();

        product = Product.builder()
                .id(10L)
                .name("Noise Cancelling Headphones")
                .slug("noise-cancelling-headphones")
                .price(new BigDecimal("2499.00"))
                .imageUrl("https://example.com/product.jpg")
                .vendorProfile(vendorProfile)
                .stockQuantity(5)
                .active(true)
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
    @DisplayName("Should merge duplicate cart items for the same product instead of creating duplicates")
    void shouldMergeDuplicateProductAdditions() {
        authenticateAs(customer);
        CartItem existingItem = CartItem.builder().id(99L).user(customer).product(product).quantity(1).build();

        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(productRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(existingItem));

        CartResponse response = cartService.addToCart(new CartItemRequest(10L, 2));

        assertThat(existingItem.getQuantity()).isEqualTo(3);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productId()).isEqualTo(10L);
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    @DisplayName("Should reject cart updates when requested quantity exceeds stock")
    void shouldRejectQuantityAboveAvailableStock() {
        authenticateAs(customer);
        CartItem item = CartItem.builder().id(22L).user(customer).product(product).quantity(2).build();

        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByIdAndUserId(22L, 1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(22L, new CartItemUpdateRequest(6)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Should reject adding inactive products to the cart")
    void shouldRejectInactiveProduct() {
        authenticateAs(customer);
        product.setActive(false);

        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(productRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(new CartItemRequest(10L, 1)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("Should remove and clear cart items for the authenticated user")
    void shouldRemoveAndClearCartItems() {
        authenticateAs(customer);
        CartItem item = CartItem.builder().id(50L).user(customer).product(product).quantity(1).build();

        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.of(item));

        cartService.removeCartItem(50L);
        verify(cartItemRepository).delete(item);

        cartService.clearCart();
        verify(cartItemRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("Should throw for items that do not belong to the current user")
    void shouldRejectCartItemBelongingToAnotherUser() {
        authenticateAs(customer);
        when(userRepository.findByEmailIgnoreCase("customer@shopstack.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByIdAndUserId(77L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeCartItem(77L))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
