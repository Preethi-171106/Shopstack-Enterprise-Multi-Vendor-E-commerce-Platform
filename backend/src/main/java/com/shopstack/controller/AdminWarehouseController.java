package com.shopstack.controller;

import com.shopstack.dto.warehouse.WarehouseCreateRequest;
import com.shopstack.dto.warehouse.WarehouseInventoryCreateRequest;
import com.shopstack.dto.warehouse.WarehouseInventoryResponse;
import com.shopstack.dto.warehouse.WarehouseInventoryUpdateRequest;
import com.shopstack.dto.warehouse.WarehouseResponse;
import com.shopstack.dto.warehouse.WarehouseUpdateRequest;
import com.shopstack.service.WarehouseManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AdminWarehouseController — REST controller for managing physical warehouses and warehouse-level inventory.
 *
 * <p>Base path: {@code /api/admin/warehouses}
 * <p>RBAC: {@code ROLE_ADMIN}
 */
@RestController
@RequestMapping("/api/admin/warehouses")
@RequiredArgsConstructor
public class AdminWarehouseController {

    private final WarehouseManagementService warehouseManagementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<List<WarehouseResponse>> getAllWarehouses(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(warehouseManagementService.getAllWarehouses(search));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<List<WarehouseResponse>> getActiveWarehouses() {
        return ResponseEntity.ok(warehouseManagementService.getActiveWarehouses());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<WarehouseResponse> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseManagementService.getWarehouseById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @Valid @RequestBody WarehouseCreateRequest request
    ) {
        WarehouseResponse response = warehouseManagementService.createWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseUpdateRequest request
    ) {
        return ResponseEntity.ok(warehouseManagementService.updateWarehouse(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseResponse> activateWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseManagementService.activateWarehouse(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseResponse> deactivateWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseManagementService.deactivateWarehouse(id));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<List<WarehouseInventoryResponse>> getAllWarehouseInventories() {
        return ResponseEntity.ok(warehouseManagementService.getAllWarehouseInventories());
    }

    @GetMapping("/{id}/inventory")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<List<WarehouseInventoryResponse>> getWarehouseInventories(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseManagementService.getWarehouseInventories(id));
    }

    @PostMapping("/{id}/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseInventoryResponse> addOrUpdateProductStock(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseInventoryCreateRequest request
    ) {
        WarehouseInventoryResponse response = warehouseManagementService.addOrUpdateProductStock(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/inventory/{productId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseInventoryResponse> adjustProductStock(
            @PathVariable Long id,
            @PathVariable Long productId,
            @Valid @RequestBody WarehouseInventoryUpdateRequest request
    ) {
        WarehouseInventoryResponse response = warehouseManagementService.adjustProductStock(id, productId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/distribute-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.shopstack.dto.warehouse.StockDistributionResponse> distributeStock(
            @Valid @RequestBody com.shopstack.dto.warehouse.StockDistributionRequest request
    ) {
        com.shopstack.dto.warehouse.StockDistributionResponse response = warehouseManagementService.distributeStock(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/distribution-overview/{productId}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<com.shopstack.dto.warehouse.StockDistributionResponse> getDistributionOverview(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(warehouseManagementService.getDistributionOverview(productId));
    }

    @GetMapping("/inventory/damaged")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<List<WarehouseInventoryResponse>> getDamagedInventories(
            @RequestParam(required = false) Long warehouseId
    ) {
        return ResponseEntity.ok(warehouseManagementService.getDamagedAndQuarantineInventories(warehouseId));
    }
}
