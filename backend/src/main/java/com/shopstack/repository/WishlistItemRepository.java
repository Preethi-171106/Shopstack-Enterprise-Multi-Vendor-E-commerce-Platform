package com.shopstack.repository;

import com.shopstack.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * WishlistItemRepository — Repository for {@link WishlistItem} entity.
 */
@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    /**
     * Finds all wishlist items for a given user ID.
     */
    List<WishlistItem> findByUserId(Long userId);

    /**
     * Finds a wishlist item by user ID and product ID.
     */
    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * Checks if a product exists in a user's wishlist.
     */
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    /**
     * Deletes a wishlist item by user ID and product ID.
     */
    void deleteByUserIdAndProductId(Long userId, Long productId);

    /**
     * Clears all wishlist items for a user.
     */
    void deleteByUserId(Long userId);
}
