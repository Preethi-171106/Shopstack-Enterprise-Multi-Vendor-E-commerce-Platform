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
public class CommissionReportResponse {
    private LocalDateTime generatedAt;
    private BigDecimal totalSalesProcessed;
    private BigDecimal totalCommissionCollected;
    private BigDecimal totalVendorPayouts;
    private long totalTransactions;
    private List<CommissionRecord> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommissionRecord {
        private Long commissionId;
        private Long orderItemId;
        private String vendorStoreName;
        private String productName;
        private BigDecimal saleAmount;
        private BigDecimal commissionRate;
        private BigDecimal commissionAmount;
        private BigDecimal vendorNetAmount;
        private String status;
        private LocalDateTime createdAt;
    }
}
