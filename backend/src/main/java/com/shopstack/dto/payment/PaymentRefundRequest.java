package com.shopstack.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * PaymentRefundRequest — DTO for requesting payment refund.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefundRequest {

    private String reason;
    private BigDecimal amount;
}
