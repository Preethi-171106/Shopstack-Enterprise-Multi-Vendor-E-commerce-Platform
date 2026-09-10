package com.shopstack.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReportResponse {
    private LocalDateTime generatedAt;
    private long totalOrders;
    private BigDecimal totalOrderValue;
    private Map<String, Long> statusCounts;
    private List<OrderRecord> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderRecord {
        private Long orderId;
        private String orderNumber;
        private LocalDateTime createdAt;
        private String customerName;
        private String customerEmail;
        private int totalItems;
        private BigDecimal totalAmount;
        private String orderStatus;
        private String paymentStatus;
    }
}
