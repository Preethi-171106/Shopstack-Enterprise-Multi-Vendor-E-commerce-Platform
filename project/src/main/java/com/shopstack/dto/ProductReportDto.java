package com.shopstack.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReportDto {

    private List<ProductSummary> bestSellingProducts;
    private List<ProductSummary> lowStockProducts;
    private List<ProductSummary> outOfStockProducts;
    private List<ProductRevenue> productRevenue;
    private BigDecimal totalProductRevenue;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProductSummary {
        private Long productId;
        private String productName;
        private Long quantitySold;
        private Integer stockQuantity;
        private BigDecimal price;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProductRevenue {
        private Long productId;
        private String productName;
        private BigDecimal revenue;
        private Long quantitySold;
    }
}
