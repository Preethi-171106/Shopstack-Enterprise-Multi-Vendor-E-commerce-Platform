package com.shopstack.repository;

import com.shopstack.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * StockMovementRepository — Data access repository for {@link StockMovement} entities.
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * Finds stock movement history for a specific product ordered by most recent first.
     */
    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    /**
     * Finds all stock movements for a specific product.
     */
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Finds stock movement history for all products belonging to a vendor profile.
     */
    Page<StockMovement> findByProductVendorProfileIdOrderByCreatedAtDesc(Long vendorProfileId, Pageable pageable);

    /**
     * Finds all stock movements ordered by most recent first.
     */
    Page<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
