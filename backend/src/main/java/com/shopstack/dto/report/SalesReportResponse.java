package com.shopstack.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportResponse {
    private LocalDateTime generatedAt;
    private BigDecimal totalGrossRevenue;
    private BigDecimal totalDiscountAmount;
    private BigDecimal totalNetRevenue;
    private long totalOrdersCount;
    private BigDecimal averageOrderValue;
    private List<SalesRecord> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesRecord {
        private String orderNumber;
        private LocalDateTime orderDate;
        private String customerEmail;
        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal totalAmount;
        private String paymentMethod;
        private String orderStatus;
    }
}
