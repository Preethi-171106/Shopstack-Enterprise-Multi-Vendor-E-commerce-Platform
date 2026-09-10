package com.shopstack.repository;

import com.shopstack.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * CartItemRepository — Repository for {@link CartItem} entity.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Finds all cart items for a given user ID.
     */
    List<CartItem> findByUserId(Long userId);

    /**
     * Finds all cart items for a given user ID with pessimistic write lock for order creation concurrency protection.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartItem c WHERE c.user.id = :userId")
    List<CartItem> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * Finds a specific product in a user's cart.
     */
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * Finds a cart item by cart item ID and user ID for ownership validation.
     */
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    /**
     * Deletes a specific cart item for a user.
     */
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user.id = :userId AND c.id = :id")
    void deleteByUserIdAndId(Long userId, Long id);

    /**
     * Clears all cart items for a user using a direct JPQL delete (no SELECT + entity delete overhead).
     */
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user.id = :userId")
    void deleteByUserId(Long userId);
}
