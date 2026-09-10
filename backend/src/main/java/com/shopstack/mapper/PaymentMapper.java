package com.shopstack.mapper;

import com.shopstack.dto.payment.PaymentResponse;
import com.shopstack.entity.Payment;
import org.springframework.stereotype.Component;

/**
 * PaymentMapper — Maps {@link Payment} entity to {@link PaymentResponse} DTO.
 */
@Component
public class PaymentMapper {

    public PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) return null;

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .orderNumber(payment.getOrder().getOrderNumber())
                .paymentId(payment.getPaymentId())
                .gatewayOrderId(payment.getGatewayOrderId())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .gateway(payment.getGateway())
                .status(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
