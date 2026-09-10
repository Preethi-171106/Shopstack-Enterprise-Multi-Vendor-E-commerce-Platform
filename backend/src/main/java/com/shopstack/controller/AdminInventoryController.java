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
 * AdminInventoryController — REST Controller for ADMIN monitoring and managing
 * platform-wide inventory and stock movements across all vendors.
 *
 * <p>Base Path: {@code /api/admin/inventory}
 * <p>RBAC: {@code ADMIN}
 */
@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<Page<InventoryResponse>> getAllInventories(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getAllInventories(pageable));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockInventories() {
        return ResponseEntity.ok(inventoryService.getLowStockInventories());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @GetMapping("/movements")
    public ResponseEntity<Page<StockMovementResponse>> getAllStockMovements(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getAllStockMovements(pageable));
    }

    @GetMapping("/product/{productId}/movements")
    public ResponseEntity<Page<StockMovementResponse>> getStockMovements(
            @PathVariable Long productId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getStockMovements(productId, pageable));
    }
}
