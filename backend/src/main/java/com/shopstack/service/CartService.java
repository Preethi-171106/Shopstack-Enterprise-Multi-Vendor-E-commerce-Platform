package com.shopstack.service;

import com.shopstack.dto.cart.CartItemRequest;
import com.shopstack.dto.cart.CartItemUpdateRequest;
import com.shopstack.dto.cart.CartResponse;
import com.shopstack.entity.CartItem;
import com.shopstack.entity.Product;
import com.shopstack.entity.User;
import com.shopstack.exception.CartItemNotFoundException;
import com.shopstack.exception.InsufficientStockException;
import com.shopstack.exception.ProductNotFoundException;
import com.shopstack.mapper.CartMapper;
import com.shopstack.repository.CartItemRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * CartService — Service managing user shopping cart operations.
 */
@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    public CartService(
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            CartMapper cartMapper
    ) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartMapper = cartMapper;
    }

    /**
     * Retrieves the shopping cart for the authenticated user.
     */
    @Transactional(readOnly = true)
    public CartResponse getCartForUser() {
        User user = resolveAuthenticatedUser();
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        return cartMapper.toCartResponse(cartItems);
    }

    /**
     * Adds an item to the authenticated user's shopping cart.
     * If the item already exists in the cart, increments the quantity.
     * Validates that product exists, is active, and stock is sufficient.
     */
    @Transactional
    public CartResponse addToCart(CartItemRequest request) {
        User user = resolveAuthenticatedUser();

        Product product = productRepository.findByIdAndActiveTrue(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with ID " + request.productId() + " not found or is inactive"
                ));

        int requestedQty = request.quantity();

        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());

        int newTotalQty = requestedQty;
        if (existingItemOpt.isPresent()) {
            newTotalQty += existingItemOpt.get().getQuantity();
        }

        if (newTotalQty > product.getStockQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + product.getName() + "'. Available: " +
                            product.getStockQuantity() + ", requested total: " + newTotalQty
            );
        }

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(newTotalQty);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(requestedQty)
                    .build();
            cartItemRepository.save(newItem);
        }

        return getCartForUser();
    }

    /**
     * Updates the quantity of an existing cart item owned by the authenticated user.
     */
    @Transactional
    public CartResponse updateCartItemQuantity(Long cartItemId, CartItemUpdateRequest request) {
        User user = resolveAuthenticatedUser();

        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item with ID " + cartItemId + " not found in user's cart"
                ));

        Product product = cartItem.getProduct();
        int newQty = request.quantity();

        if (newQty > product.getStockQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + product.getName() + "'. Available: " +
                            product.getStockQuantity() + ", requested: " + newQty
            );
        }

        cartItem.setQuantity(newQty);
        cartItemRepository.save(cartItem);

        return getCartForUser();
    }

    /**
     * Removes a single cart item owned by the authenticated user.
     */
    @Transactional
    public void removeCartItem(Long cartItemId) {
        User user = resolveAuthenticatedUser();

        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item with ID " + cartItemId + " not found in user's cart"
                ));

        cartItemRepository.delete(cartItem);
    }

    /**
     * Clears all items in the authenticated user's shopping cart.
     */
    @Transactional
    public void clearCart() {
        User user = resolveAuthenticatedUser();
        cartItemRepository.deleteByUserId(user.getId());
    }

    /**
     * Resolves the currently authenticated user from SecurityContext.
     */
    private User resolveAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
