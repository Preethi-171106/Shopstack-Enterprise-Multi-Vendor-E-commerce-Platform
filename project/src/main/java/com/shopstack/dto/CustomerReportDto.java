package com.shopstack.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerReportDto {

    private Long totalCustomers;
    private Long newCustomers;
    private BigDecimal totalCustomerSpending;
    private BigDecimal averageCustomerSpending;
    private List<CustomerSummary> topCustomers;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CustomerSummary {
        private Long customerId;
        private String customerName;
        private String email;
        private BigDecimal totalSpent;
        private Long ordersCount;
    }
}
