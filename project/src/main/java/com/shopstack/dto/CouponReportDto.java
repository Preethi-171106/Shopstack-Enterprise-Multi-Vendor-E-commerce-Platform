package com.shopstack.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponReportDto {

    private List<CouponSummary> coupons;
    private BigDecimal totalDiscount;
    private Long totalUsage;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CouponSummary {
        private Long couponId;
        private String code;
        private Long usageCount;
        private BigDecimal totalDiscount;
        private BigDecimal discountPercentage;
    }
}
