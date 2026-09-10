package com.shopstack.controller;

import com.shopstack.dto.warehouse.AllocationActionRequest;
import com.shopstack.dto.warehouse.AllocationResponse;
import com.shopstack.service.WarehouseAllocationService;
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
 * WarehouseAllocationController — REST controller for managing item-level warehouse allocations,
 * picking, packing, and dispatch staging.
 *
 * <p>Base path: {@code /api/warehouse/allocations}
 * <p>RBAC: {@code ROLE_WAREHOUSE_STAFF}, {@code ROLE_ADMIN}
 */
@RestController
@RequestMapping("/api/warehouse/allocations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
public class WarehouseAllocationController {

    private final WarehouseAllocationService warehouseAllocationService;

    @GetMapping
    public ResponseEntity<List<AllocationResponse>> getAllocations(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(warehouseAllocationService.getAllocations(warehouseId, status));
    }

    @GetMapping("/picking")
    public ResponseEntity<List<AllocationResponse>> getPickingAllocations(
            @RequestParam(required = false) Long warehouseId
    ) {
        return ResponseEntity.ok(warehouseAllocationService.getPickingAllocations(warehouseId));
    }

    @GetMapping("/packing")
    public ResponseEntity<List<AllocationResponse>> getPackingAllocations(
            @RequestParam(required = false) Long warehouseId
    ) {
        return ResponseEntity.ok(warehouseAllocationService.getPackingAllocations(warehouseId));
    }

    @GetMapping({"/ready-to-ship", "/ready-for-shipment"})
    public ResponseEntity<List<AllocationResponse>> getReadyToShipAllocations(
            @RequestParam(required = false) Long warehouseId
    ) {
        return ResponseEntity.ok(warehouseAllocationService.getReadyToShipAllocations(warehouseId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AllocationResponse> getAllocationById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseAllocationService.getAllocationById(id));
    }

    @PostMapping("/{id}/pick")
    public ResponseEntity<AllocationResponse> pickAllocation(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseAllocationService.pickAllocation(id));
    }

    @PostMapping("/{id}/pack")
    public ResponseEntity<AllocationResponse> packAllocation(
            @PathVariable Long id,
            @RequestBody(required = false) AllocationActionRequest request
    ) {
        return ResponseEntity.ok(warehouseAllocationService.packAllocation(id, request));
    }

    @PostMapping({"/{id}/ready-for-shipment", "/{id}/ready-to-ship"})
    public ResponseEntity<AllocationResponse> readyForShipment(
            @PathVariable Long id,
            @RequestBody(required = false) AllocationActionRequest request
    ) {
        return ResponseEntity.ok(warehouseAllocationService.readyForShipment(id, request));
    }
}
