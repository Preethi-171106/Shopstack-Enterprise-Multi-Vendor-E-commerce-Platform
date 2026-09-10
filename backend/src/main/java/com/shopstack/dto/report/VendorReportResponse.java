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
public class VendorReportResponse {
    private LocalDateTime generatedAt;
    private long totalVendorsCount;
    private long activeVendorsCount;
    private BigDecimal totalMarketplaceGrossSales;
    private BigDecimal totalPlatformCommissionGenerated;
    private List<VendorRecord> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VendorRecord {
        private Long vendorId;
        private String storeName;
        private String ownerEmail;
        private String status;
        private long totalProducts;
        private long itemsSold;
        private BigDecimal grossSales;
        private BigDecimal commissionPaid;
        private BigDecimal netEarnings;
    }
}
