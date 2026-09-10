package com.shopstack.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorReportDto {

    private List<VendorSummary> vendors;
    private List<VendorSummary> topVendors;
    private BigDecimal totalVendorRevenue;
    private Long totalVendorOrders;
    private Long totalVendorProducts;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VendorSummary {
        private Long vendorId;
        private String vendorName;
        private BigDecimal revenue;
        private Long ordersCount;
        private Long productsCount;
    }
}
