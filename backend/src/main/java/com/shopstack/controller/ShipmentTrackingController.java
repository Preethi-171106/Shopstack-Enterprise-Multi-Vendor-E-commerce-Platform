package com.shopstack.controller;

import com.shopstack.dto.shipment.ShipmentResponse;
import com.shopstack.service.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ShipmentTrackingController — Public-facing shipment tracking lookup by tracking number.
 *
 * <p>Access: Any authenticated user may look up a shipment by tracking number.
 */
@RestController
@RequestMapping("/api/shipments")
public class ShipmentTrackingController {

    private final ShipmentService shipmentService;

    public ShipmentTrackingController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /**
     * Returns shipment details and full tracking history by tracking number.
     * GET /api/shipments/tracking/{trackingNumber} — 200 OK
     */
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> trackShipment(@PathVariable String trackingNumber) {
        ShipmentResponse response = shipmentService.getShipmentByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(response);
    }
}
