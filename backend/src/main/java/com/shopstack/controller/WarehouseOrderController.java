package com.shopstack.controller;

import com.shopstack.dto.warehouse.WarehouseDashboardResponse;
import com.shopstack.dto.warehouse.WarehouseInventoryResponse;
import com.shopstack.dto.warehouse.WarehouseOrderResponse;
import com.shopstack.dto.warehouse.WarehousePackingRequest;
import com.shopstack.dto.warehouse.WarehouseReadyToShipRequest;
import com.shopstack.dto.warehouse.WarehouseResponse;
import com.shopstack.service.WarehouseManagementService;
import com.shopstack.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WarehouseOrderController — REST Controller for warehouse operations (dashboard, facilities, order queue, picking, packing, shipment staging).
 *
 * <p>Base Path: {@code /api/warehouse}
 * <p>RBAC: {@code WAREHOUSE_STAFF}, {@code ADMIN}
 */
@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
public class WarehouseOrderController {

    private final WarehouseService warehouseService;
    private final WarehouseManagementService warehouseManagementService;

    @GetMapping("/dashboard")
    public ResponseEntity<WarehouseDashboardResponse> getWarehouseDashboard() {
        return ResponseEntity.ok(warehouseService.getWarehouseDashboard());
    }

    @GetMapping({"/facilities", "/warehouses"})
    public ResponseEntity<List<WarehouseResponse>> getFacilities(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(warehouseManagementService.getAllWarehouses(search));
    }

    @GetMapping({"/facilities/active", "/warehouses/active"})
    public ResponseEntity<List<WarehouseResponse>> getActiveFacilities() {
        return ResponseEntity.ok(warehouseManagementService.getActiveWarehouses());
    }

    @GetMapping({"/facilities/{id:\\d+}", "/warehouses/{id:\\d+}"})
    public ResponseEntity<WarehouseResponse> getFacilityById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseManagementService.getWarehouseById(id));
    }

    @GetMapping({"/facilities/inventory", "/warehouses/inventory", "/facility-inventories"})
    public ResponseEntity<List<WarehouseInventoryResponse>> getAllFacilityInventories() {
        return ResponseEntity.ok(warehouseManagementService.getAllWarehouseInventories());
    }

    @GetMapping({"/facilities/{id:\\d+}/inventory", "/warehouses/{id:\\d+}/inventory"})
    public ResponseEntity<List<WarehouseInventoryResponse>> getFacilityInventories(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseManagementService.getWarehouseInventories(id));
    }

    @GetMapping({"/facilities/damaged", "/warehouses/damaged", "/inventory/damaged"})
    public ResponseEntity<List<WarehouseInventoryResponse>> getDamagedInventories(
            @RequestParam(required = false) Long warehouseId
    ) {
        return ResponseEntity.ok(warehouseManagementService.getDamagedAndQuarantineInventories(warehouseId));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<WarehouseOrderResponse>> getWarehouseOrders(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(warehouseService.getWarehouseOrders(status));
    }

    @GetMapping("/picking/orders")
    public ResponseEntity<List<WarehouseOrderResponse>> getPickingOrders() {
        return ResponseEntity.ok(warehouseService.getPickingOrders());
    }

    @GetMapping("/packing/orders")
    public ResponseEntity<List<WarehouseOrderResponse>> getPackingOrders() {
        return ResponseEntity.ok(warehouseService.getPackingOrders());
    }

    @GetMapping({"/ready-to-ship/orders", "/shipment-prep/orders"})
    public ResponseEntity<List<WarehouseOrderResponse>> getReadyToShipOrders() {
        return ResponseEntity.ok(warehouseService.getReadyToShipOrders());
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<WarehouseOrderResponse> getWarehouseOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(warehouseService.getWarehouseOrderById(orderId));
    }

    @PostMapping("/orders/{orderId}/start-picking")
    public ResponseEntity<WarehouseOrderResponse> startPicking(@PathVariable Long orderId) {
        return ResponseEntity.ok(warehouseService.startPicking(orderId));
    }

    @PostMapping({"/orders/{orderId}/mark-picked", "/orders/{orderId}/complete-picking"})
    public ResponseEntity<WarehouseOrderResponse> markPicked(@PathVariable Long orderId) {
        return ResponseEntity.ok(warehouseService.markPicked(orderId));
    }

    @PostMapping("/orders/{orderId}/start-packing")
    public ResponseEntity<WarehouseOrderResponse> startPacking(@PathVariable Long orderId) {
        return ResponseEntity.ok(warehouseService.startPacking(orderId));
    }

    @PostMapping({"/orders/{orderId}/mark-packed", "/orders/{orderId}/complete-packing"})
    public ResponseEntity<WarehouseOrderResponse> markPacked(
            @PathVariable Long orderId,
            @RequestBody(required = false) WarehousePackingRequest request
    ) {
        return ResponseEntity.ok(warehouseService.markPacked(orderId, request));
    }

    @PostMapping({"/orders/{orderId}/ready-to-ship", "/orders/{orderId}/prepare-shipment"})
    public ResponseEntity<WarehouseOrderResponse> readyToShip(
            @PathVariable Long orderId,
            @Valid @RequestBody WarehouseReadyToShipRequest request
    ) {
        return ResponseEntity.ok(warehouseService.readyToShip(orderId, request));
    }
}

