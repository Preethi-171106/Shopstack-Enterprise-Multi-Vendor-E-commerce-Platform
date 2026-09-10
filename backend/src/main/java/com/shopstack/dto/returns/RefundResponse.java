package com.shopstack.dto.returns;

import com.shopstack.entity.PaymentGateway;
import com.shopstack.entity.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private Long id;
    private String refundId;
    private Long paymentId;
    private Long returnRequestId;
    private String returnNumber;
    private BigDecimal amount;
    private PaymentGateway gateway;
    private RefundStatus status;
    private String transactionId;
    private String remarks;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
