package com.shopstack.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReportDto {

    private Long successfulPayments;
    private Long failedPayments;
    private Long refundedPayments;
    private BigDecimal successfulAmount;
    private BigDecimal refundedAmount;
    private List<PaymentMethodStat> paymentMethodStats;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentMethodStat {
        private String paymentMethod;
        private Long count;
        private BigDecimal amount;
    }
}
