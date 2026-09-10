package com.shopstack.dto.payment;

import com.shopstack.entity.PaymentGateway;
import com.shopstack.entity.PaymentMethod;
import com.shopstack.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentResponse — DTO representing payment details sent to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private String paymentId;
    private String gatewayOrderId;
    private String gatewayTransactionId;

    /** Amount in major currency unit (e.g., 999.99 for ₹999.99). */
    private BigDecimal amount;

    /** Amount in smallest currency unit (paise) — convenient for frontend Razorpay SDK. */
    private Long amountInPaise;

    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentGateway gateway;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Exposes the public Razorpay Key ID (safe to send to clients). Never include the secret.
     */
    private String razorpayKeyId;
}
