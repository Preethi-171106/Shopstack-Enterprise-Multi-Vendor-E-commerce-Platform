package com.shopstack.controller;

import com.shopstack.dto.inventory.InventoryResponse;
import com.shopstack.dto.inventory.StockMovementResponse;
import com.shopstack.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * VendorInventoryController — REST Controller for VENDOR users to view stock levels
 * and stock movement audit history strictly for their own listed products.
 *
 * <p>Base Path: {@code /api/vendor/inventory}
 * <p>RBAC: {@code VENDOR}
 */
@RestController
@RequestMapping("/api/vendor/inventory")
public class VendorInventoryController {

    private final InventoryService inventoryService;

    public VendorInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<Page<InventoryResponse>> getVendorInventories(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getVendorInventories(pageable));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getVendorLowStockInventories() {
        return ResponseEntity.ok(inventoryService.getVendorLowStockInventories());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @GetMapping("/movements")
    public ResponseEntity<Page<StockMovementResponse>> getVendorStockMovements(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getVendorStockMovements(pageable));
    }

    @GetMapping("/product/{productId}/movements")
    public ResponseEntity<Page<StockMovementResponse>> getStockMovements(
            @PathVariable Long productId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getStockMovements(productId, pageable));
    }
}
