package com.shopstack.controller;

import com.shopstack.dto.shipment.ShipmentResponse;
import com.shopstack.service.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ShipmentCustomerController — REST controller for customer-scoped shipment access.
 *
 * <p>Access: CUSTOMER only (enforced in SecurityConfig). Customers may only view
 * shipments for orders they placed.
 */
@RestController
@RequestMapping("/api/customer/shipments")
public class ShipmentCustomerController {

    private final ShipmentService shipmentService;

    public ShipmentCustomerController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /**
     * Lists all shipments for the authenticated customer's orders.
     * GET /api/customer/shipments — 200 OK
     */
    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getMyShipments() {
        List<ShipmentResponse> shipments = shipmentService.getCustomerShipments();
        return ResponseEntity.ok(shipments);
    }

    /**
     * Gets the shipment for a specific order owned by the authenticated customer.
     * GET /api/customer/shipments/order/{orderId} — 200 OK
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ShipmentResponse> getMyShipmentByOrderId(@PathVariable Long orderId) {
        ShipmentResponse response = shipmentService.getCustomerShipmentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }
}
