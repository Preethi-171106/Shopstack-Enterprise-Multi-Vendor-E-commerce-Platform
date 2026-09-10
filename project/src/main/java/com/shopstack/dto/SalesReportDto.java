package com.shopstack.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportDto {

    private LocalDateRange period;
    private BigDecimal revenue;
    private Long ordersCount;
    private Long cancelledOrders;
    private Long refundedOrders;
    private BigDecimal averageOrderValue;
    private List<PeriodBreakdown> breakdown;
    private List<StatusCount> statusCounts;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PeriodBreakdown {
        private String periodLabel;
        private BigDecimal revenue;
        private Long ordersCount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StatusCount {
        private String status;
        private Long count;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LocalDateRange {
        private String from;
        private String to;
    }
}
