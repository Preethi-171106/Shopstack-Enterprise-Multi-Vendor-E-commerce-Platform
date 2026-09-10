package com.shopstack.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * CommissionSummaryResponse — platform-wide commission overview with per-vendor breakdown.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionSummaryResponse {
    private BigDecimal totalPlatformCommission;
    private BigDecimal totalGrossSales;
    private BigDecimal totalVendorPayouts;
    private BigDecimal defaultCommissionRatePercent;
    private long totalCommissionRecords;

    private List<VendorCommissionItem> vendorBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VendorCommissionItem {
        private Long vendorProfileId;
        private String vendorStoreName;
        private BigDecimal totalSales;
        private BigDecimal commissionAmount;
        private BigDecimal netPayout;
        private long orderItemCount;
    }
}
