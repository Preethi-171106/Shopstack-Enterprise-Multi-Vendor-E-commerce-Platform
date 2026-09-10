package com.shopstack.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReportDto {

    private Long totalProducts;
    private Long lowStockCount;
    private Long outOfStockCount;
    private BigDecimal stockValue;
    private List<InventoryItem> lowStockItems;
    private List<InventoryItem> outOfStockItems;
    private List<InventoryItem> summary;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InventoryItem {
        private Long productId;
        private String productName;
        private Integer stockQuantity;
        private Integer lowStockThreshold;
        private BigDecimal unitPrice;
        private BigDecimal stockValue;
        private String status;
    }
}
