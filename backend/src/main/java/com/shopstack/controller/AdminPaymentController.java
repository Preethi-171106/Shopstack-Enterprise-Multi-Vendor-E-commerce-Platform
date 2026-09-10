package com.shopstack.controller;

import com.shopstack.dto.payment.PaymentResponse;
import com.shopstack.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AdminPaymentController — REST Controller for ADMIN-scoped payment management.
 *
 * <p>Base Path: {@code /api/admin/payments}
 * <p>RBAC: {@code ADMIN} role only (enforced in SecurityConfig).
 *
 * <ul>
 *   <li>GET /api/admin/payments      — List all payments in the platform</li>
 *   <li>GET /api/admin/payments/{id} — Get a specific payment by ID</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Lists all payment transactions across the platform, ordered by creation date descending.
     * GET /api/admin/payments — 200 OK
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * Retrieves a specific payment record by its primary key.
     * GET /api/admin/payments/{id} — 200 OK
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }
}
