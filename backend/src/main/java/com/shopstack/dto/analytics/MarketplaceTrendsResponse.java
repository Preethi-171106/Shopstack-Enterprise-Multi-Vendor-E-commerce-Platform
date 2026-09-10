package com.shopstack.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * MarketplaceTrendsResponse — time-series and categorical analytics metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceTrendsResponse {
    private List<DailyTrend> dailySalesTrend;
    private List<MonthlyTrend> monthlySalesTrend;
    private Map<String, Long> orderStatusDistribution;
    private List<CategorySales> topCategories;
    private List<VendorSalesPerformance> topVendors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyTrend {
        private String date; // YYYY-MM-DD
        private BigDecimal revenue;
        private long orderCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyTrend {
        private String month; // YYYY-MM
        private BigDecimal revenue;
        private long orderCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategorySales {
        private String categoryName;
        private long productCount;
        private BigDecimal salesAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VendorSalesPerformance {
        private Long vendorId;
        private String storeName;
        private BigDecimal totalSales;
        private long orderCount;
    }
}
