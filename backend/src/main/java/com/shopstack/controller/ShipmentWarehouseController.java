package com.shopstack.controller;

import com.shopstack.dto.shipment.ShipmentCreateRequest;
import com.shopstack.dto.shipment.ShipmentResponse;
import com.shopstack.dto.shipment.ShipmentStatusUpdateRequest;
import com.shopstack.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ShipmentWarehouseController — REST controller for warehouse staff and admin shipment operations.
 *
 * <p>Access: WAREHOUSE_STAFF or ADMIN (enforced in SecurityConfig).
 */
@RestController
@RequestMapping("/api/warehouse/shipments")
public class ShipmentWarehouseController {

    private final ShipmentService shipmentService;

    public ShipmentWarehouseController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /**
     * Creates a new shipment for an order.
     * POST /api/warehouse/shipments — 201 Created
     */
    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody ShipmentCreateRequest request
    ) {
        ShipmentResponse response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all shipments in the system.
     * GET /api/warehouse/shipments — 200 OK
     */
    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getAllShipments() {
        List<ShipmentResponse> shipments = shipmentService.getAllShipments();
        return ResponseEntity.ok(shipments);
    }

    /**
     * Gets a shipment by ID.
     * GET /api/warehouse/shipments/{id} — 200 OK
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable Long id) {
        ShipmentResponse response = shipmentService.getShipmentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates shipment status and appends a tracking event.
     * PATCH /api/warehouse/shipments/{id}/status — 200 OK
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusUpdateRequest request
    ) {
        ShipmentResponse response = shipmentService.updateShipmentStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
