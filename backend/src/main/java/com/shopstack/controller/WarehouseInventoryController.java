package com.shopstack.controller;

import com.shopstack.dto.inventory.InventoryCreateRequest;
import com.shopstack.dto.inventory.InventoryResponse;
import com.shopstack.dto.inventory.InventoryThresholdUpdateRequest;
import com.shopstack.dto.inventory.StockAddRequest;
import com.shopstack.dto.inventory.StockAdjustmentRequest;
import com.shopstack.dto.inventory.StockMovementResponse;
import com.shopstack.dto.inventory.StockReleaseRequest;
import com.shopstack.dto.inventory.StockReserveRequest;
import com.shopstack.dto.inventory.StockReturnRequest;
import com.shopstack.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WarehouseInventoryController — REST Controller for warehouse staff and admins
 * to perform inventory management operations (stock IN, OUT, reserve, release, adjust, return).
 *
 * <p>Base Path: {@code /api/warehouse/inventory}
 * <p>RBAC: {@code WAREHOUSE_STAFF}, {@code ADMIN}
 */
@RestController
@RequestMapping("/api/warehouse")
@PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
public class WarehouseInventoryController {

    private final InventoryService inventoryService;

    public WarehouseInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/inventory")
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody InventoryCreateRequest request) {
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<InventoryResponse>> getAllInventories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        if ((search != null && !search.isBlank()) || (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL"))) {
            return ResponseEntity.ok(inventoryService.getInventoriesWithFilters(search, status));
        }
        return ResponseEntity.ok(inventoryService.getAllInventories());
    }

    @GetMapping("/inventory/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockInventories() {
        return ResponseEntity.ok(inventoryService.getLowStockInventories());
    }

    @GetMapping("/inventory/out-of-stock")
    public ResponseEntity<List<InventoryResponse>> getOutOfStockInventories() {
        return ResponseEntity.ok(inventoryService.getOutOfStockInventories());
    }

    @GetMapping({"/inventory/{productId:\\d+}", "/inventory/product/{productId:\\d+}"})
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @PostMapping({"/inventory/{productId:\\d+}/add", "/inventory/product/{productId:\\d+}/add"})
    public ResponseEntity<InventoryResponse> addStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockAddRequest request
    ) {
        return ResponseEntity.ok(inventoryService.addStock(productId, request));
    }

    @PostMapping({"/inventory/{productId:\\d+}/adjust", "/inventory/product/{productId:\\d+}/adjust"})
    public ResponseEntity<InventoryResponse> adjustStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return ResponseEntity.ok(inventoryService.adjustStock(productId, request));
    }

    @PostMapping({"/inventory/{productId:\\d+}/reserve", "/inventory/product/{productId:\\d+}/reserve"})
    public ResponseEntity<InventoryResponse> reserveStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockReserveRequest request
    ) {
        return ResponseEntity.ok(inventoryService.reserveStock(productId, request));
    }

    @PostMapping({"/inventory/{productId:\\d+}/release", "/inventory/product/{productId:\\d+}/release"})
    public ResponseEntity<InventoryResponse> releaseReservedStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockReleaseRequest request
    ) {
        return ResponseEntity.ok(inventoryService.releaseReservedStock(productId, request));
    }

    @PostMapping({"/inventory/{productId:\\d+}/return", "/inventory/product/{productId:\\d+}/return"})
    public ResponseEntity<InventoryResponse> recordReturnStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockReturnRequest request
    ) {
        return ResponseEntity.ok(inventoryService.recordReturnStock(productId, request));
    }

    @PutMapping({"/inventory/{productId:\\d+}/threshold", "/inventory/product/{productId:\\d+}/threshold"})
    public ResponseEntity<InventoryResponse> updateLowStockThreshold(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryThresholdUpdateRequest request
    ) {
        return ResponseEntity.ok(inventoryService.updateLowStockThreshold(productId, request));
    }

    @GetMapping({"/stock-movements", "/inventory/movements"})
    public ResponseEntity<Page<StockMovementResponse>> getAllStockMovements(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getAllStockMovements(pageable));
    }

    @GetMapping({"/stock-movements/product/{productId:\\d+}", "/inventory/product/{productId:\\d+}/movements"})
    public ResponseEntity<Page<StockMovementResponse>> getStockMovements(
            @PathVariable Long productId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getStockMovements(productId, pageable));
    }
}

