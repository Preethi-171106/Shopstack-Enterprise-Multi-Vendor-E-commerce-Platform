package com.shopstack.service;

import com.shopstack.dto.payment.PaymentCreateRequest;
import com.shopstack.dto.payment.PaymentRefundRequest;
import com.shopstack.dto.payment.PaymentResponse;
import com.shopstack.dto.payment.PaymentVerifyRequest;

import java.util.List;

/**
 * PaymentService — Service interface for payment lifecycle operations.
 */
public interface PaymentService {

    /**
     * Creates a Razorpay order and stores a payment record for the given order.
     *
     * @param orderId  the ID of the order to pay
     * @param request  the payment creation request
     * @return the created {@link PaymentResponse}
     */
    PaymentResponse createPayment(Long orderId, PaymentCreateRequest request);

    /**
     * Verifies the Razorpay payment signature and updates the payment status.
     *
     * @param request  the verification request containing Razorpay IDs and signature
     * @return the updated {@link PaymentResponse}
     */
    PaymentResponse verifyPayment(PaymentVerifyRequest request);

    /**
     * Retrieves payment details for the specified order.
     *
     * @param orderId  the order ID
     * @return the {@link PaymentResponse} for the order
     */
    PaymentResponse getPaymentByOrder(Long orderId);

    /**
     * Refunds a successful payment for the specified order.
     *
     * @param orderId  the order ID
     * @param request  the refund request (optional reason)
     * @return the updated {@link PaymentResponse} with REFUNDED status
     */
    PaymentResponse refundPayment(Long orderId, PaymentRefundRequest request);

    /**
     * Returns all payment records (Admin-only).
     *
     * @return list of all {@link PaymentResponse}
     */
    List<PaymentResponse> getAllPayments();

    /**
     * Returns a single payment by its primary key (Admin-only).
     *
     * @param id  the payment ID
     * @return the {@link PaymentResponse}
     */
    PaymentResponse getPaymentById(Long id);
}
