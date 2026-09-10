package com.shopstack.service;

import com.shopstack.dto.wishlist.WishlistItemResponse;
import com.shopstack.entity.Product;
import com.shopstack.entity.User;
import com.shopstack.entity.WishlistItem;
import com.shopstack.exception.ProductNotFoundException;
import com.shopstack.exception.WishlistItemNotFoundException;
import com.shopstack.mapper.WishlistMapper;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.WishlistItemRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * WishlistService — Service managing user saved wishlist operations.
 */
@Service
public class WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WishlistMapper wishlistMapper;

    public WishlistService(
            WishlistItemRepository wishlistRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            WishlistMapper wishlistMapper
    ) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.wishlistMapper = wishlistMapper;
    }

    /**
     * Retrieves all saved wishlist items for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlistForUser() {
        User user = resolveAuthenticatedUser();
        return wishlistRepository.findByUserId(user.getId())
                .stream()
                .map(wishlistMapper::toResponse)
                .toList();
    }

    /**
     * Adds a product to the authenticated user's wishlist. Idempotent.
     */
    @Transactional
    public WishlistItemResponse addToWishlist(Long productId) {
        User user = resolveAuthenticatedUser();

        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with ID " + productId + " not found or is inactive"
                ));

        return wishlistRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .map(wishlistMapper::toResponse)
                .orElseGet(() -> {
                    WishlistItem newItem = WishlistItem.builder()
                            .user(user)
                            .product(product)
                            .build();
                    WishlistItem saved = wishlistRepository.save(newItem);
                    return wishlistMapper.toResponse(saved);
                });
    }

    /**
     * Removes a product from the authenticated user's wishlist.
     */
    @Transactional
    public void removeFromWishlist(Long productId) {
        User user = resolveAuthenticatedUser();

        if (!wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new WishlistItemNotFoundException(
                    "Product with ID " + productId + " is not in user's wishlist"
            );
        }

        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
    }

    /**
     * Clears all saved wishlist items for the authenticated user.
     */
    @Transactional
    public void clearWishlist() {
        User user = resolveAuthenticatedUser();
        wishlistRepository.deleteByUserId(user.getId());
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
