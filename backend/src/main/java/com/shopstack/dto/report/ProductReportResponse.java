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
public class ProductReportResponse {
    private LocalDateTime generatedAt;
    private long totalProducts;
    private long activeProducts;
    private long lowStockProducts;
    private long outOfStockProducts;
    private List<ProductRecord> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductRecord {
        private Long productId;
        private String sku;
        private String name;
        private String categoryName;
        private String vendorStoreName;
        private BigDecimal price;
        private int stockQuantity;
        private boolean active;
        private boolean lowStock;
    }
}
