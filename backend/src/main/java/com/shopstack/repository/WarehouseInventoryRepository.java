package com.shopstack.repository;

import com.shopstack.entity.WarehouseInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory, Long> {

    Optional<WarehouseInventory> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<WarehouseInventory> findByProductId(Long productId);

    List<WarehouseInventory> findByProductIdAndWarehouseActiveTrue(Long productId);

    List<WarehouseInventory> findByWarehouseId(Long warehouseId);

    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.product.id = :productId " +
           "AND wi.warehouse.active = true " +
           "AND wi.availableQuantity >= :requiredQuantity " +
           "ORDER BY wi.availableQuantity DESC, wi.warehouse.id ASC")
    List<WarehouseInventory> findEligibleWarehousesForProduct(
            @Param("productId") Long productId,
            @Param("requiredQuantity") int requiredQuantity
    );

    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.warehouse.id = :warehouseId " +
           "AND (wi.totalQuantity <= wi.lowStockThreshold OR wi.availableQuantity = 0)")
    List<WarehouseInventory> findLowStockByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.damagedQuantity > 0 OR wi.quarantineQuantity > 0")
    List<WarehouseInventory> findAllDamagedAndQuarantineInventories();

    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.warehouse.id = :warehouseId AND (wi.damagedQuantity > 0 OR wi.quarantineQuantity > 0)")
    List<WarehouseInventory> findDamagedAndQuarantineByWarehouseId(@Param("warehouseId") Long warehouseId);
}
