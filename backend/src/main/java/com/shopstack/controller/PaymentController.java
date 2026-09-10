package com.shopstack.controller;

import com.shopstack.dto.payment.PaymentCreateRequest;
import com.shopstack.dto.payment.PaymentRefundRequest;
import com.shopstack.dto.payment.PaymentResponse;
import com.shopstack.dto.payment.PaymentVerifyRequest;
import com.shopstack.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PaymentController — REST Controller for customer-facing payment operations.
 *
 * <p>Access: CUSTOMER role only (enforced in SecurityConfig).</p>
 *
 * <ul>
 *   <li>POST /api/payments/create/{orderId} — Create a Razorpay payment order</li>
 *   <li>POST /api/payments/verify          — Verify Razorpay signature and capture</li>
 *   <li>GET  /api/payments/order/{orderId} — Get payment details by order</li>
 *   <li>POST /api/payments/refund/{orderId}— Refund a completed payment</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Creates a payment order with Razorpay for the given application order.
     * POST /api/payments/create/{orderId} — 201 Created
     */
    @PostMapping("/create/{orderId}")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentCreateRequest request
    ) {
        PaymentResponse response = paymentService.createPayment(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Verifies the Razorpay signature and marks the payment as SUCCESS or FAILED.
     * POST /api/payments/verify — 200 OK
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request
    ) {
        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves payment details for the authenticated customer's order.
     * GET /api/payments/order/{orderId} — 200 OK
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentByOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Requests a refund on a successfully paid order.
     * POST /api/payments/refund/{orderId} — 200 OK
     */
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long orderId,
            @RequestBody(required = false) PaymentRefundRequest request
    ) {
        PaymentResponse response = paymentService.refundPayment(orderId, request);
        return ResponseEntity.ok(response);
    }
}
