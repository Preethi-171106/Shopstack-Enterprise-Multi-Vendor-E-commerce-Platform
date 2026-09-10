package com.shopstack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * RoleTestController — Controller for security verification of role-based authorization.
 *
 * <p>Base URL: {@code /api/test}
 */
@RestController
@RequestMapping("/api/test")
public class RoleTestController {

    @GetMapping("/customer")
    public ResponseEntity<Map<String, String>> customerEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "Customer access granted",
                "role", "CUSTOMER"
        ));
    }

    @GetMapping("/vendor")
    public ResponseEntity<Map<String, String>> vendorEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "Vendor access granted",
                "role", "VENDOR"
        ));
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, String>> adminEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "Admin access granted",
                "role", "ADMIN"
        ));
    }

    @GetMapping("/warehouse")
    public ResponseEntity<Map<String, String>> warehouseEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "Warehouse staff access granted",
                "role", "WAREHOUSE_STAFF"
        ));
    }
}
