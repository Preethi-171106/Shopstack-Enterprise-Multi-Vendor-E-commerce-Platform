package com.shopstack.repository;

import com.shopstack.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * InventoryRepository — Data access repository for {@link Inventory} entities.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Finds inventory record by product ID.
     */
    Optional<Inventory> findByProductId(Long productId);

    /**
     * Checks if inventory record exists for product ID.
     */
    boolean existsByProductId(Long productId);

    /**
     * Finds all inventories where totalStock is less than or equal to lowStockThreshold.
     */
    @Query("SELECT i FROM Inventory i WHERE i.totalStock <= i.lowStockThreshold")
    List<Inventory> findLowStockInventories();

    /**
     * Finds all inventories for products belonging to a specific vendor.
     */
    List<Inventory> findByProductVendorProfileId(Long vendorProfileId);

    /**
     * Finds paged inventories for products belonging to a specific vendor.
     */
    Page<Inventory> findByProductVendorProfileId(Long vendorProfileId, Pageable pageable);

    /**
     * Finds all inventories where availableStock is 0 or totalStock is 0.
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableStock <= 0 OR i.totalStock <= 0")
    List<Inventory> findOutOfStockInventories();

    /**
     * Sums total physical stock across all product inventories.
     */
    @Query("SELECT COALESCE(SUM(i.totalStock), 0) FROM Inventory i")
    long sumTotalStock();

    /**
     * Counts out of stock inventories.
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.availableStock <= 0 OR i.totalStock <= 0")
    long countOutOfStock();

    /**
     * Counts low stock inventories.
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.totalStock <= i.lowStockThreshold")
    long countLowStock();

    /**
     * Finds low stock inventories for a specific vendor.
     */
    @Query("SELECT i FROM Inventory i WHERE i.product.vendorProfile.id = :vendorProfileId AND i.totalStock <= i.lowStockThreshold")
    List<Inventory> findLowStockInventoriesByVendorProfileId(@Param("vendorProfileId") Long vendorProfileId);

    /**
     * Searches inventories by product name or SKU (case-insensitive).
     */
    @Query("SELECT i FROM Inventory i WHERE LOWER(i.product.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.product.sku) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Inventory> searchInventories(@Param("search") String search);
}
