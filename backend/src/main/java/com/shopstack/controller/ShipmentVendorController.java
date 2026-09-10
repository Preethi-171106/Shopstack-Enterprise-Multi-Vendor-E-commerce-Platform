package com.shopstack.controller;

import com.shopstack.dto.shipment.ShipmentResponse;
import com.shopstack.service.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ShipmentVendorController — REST controller for vendor-scoped shipment visibility.
 *
 * <p>Access: VENDOR only (enforced in SecurityConfig). Vendors may only see
 * shipments that contain products from their store.
 */
@RestController
@RequestMapping("/api/vendor/shipments")
public class ShipmentVendorController {

    private final ShipmentService shipmentService;

    public ShipmentVendorController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /**
     * Lists all shipments containing products from the authenticated vendor's store.
     * GET /api/vendor/shipments — 200 OK
     */
    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getMyVendorShipments() {
        List<ShipmentResponse> shipments = shipmentService.getVendorShipments();
        return ResponseEntity.ok(shipments);
    }
}
