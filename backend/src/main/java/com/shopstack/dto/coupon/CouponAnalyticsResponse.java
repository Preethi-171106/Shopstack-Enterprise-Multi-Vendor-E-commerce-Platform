package com.shopstack.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponAnalyticsResponse {

    private long totalCoupons;
    private long activeCoupons;
    private long expiredCoupons;
    private long disabledCoupons;
    private long totalUsageCount;
    private BigDecimal totalDiscountGiven;
    private String topCouponCode;
    private long topCouponUsageCount;
    private List<CouponPerformanceDto> couponBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CouponPerformanceDto {
        private Long couponId;
        private String code;
        private String name;
        private String discountType;
        private BigDecimal discountValue;
        private Integer usedCount;
        private Integer usageLimit;
        private BigDecimal totalDiscountAmount;
        private String status;
    }
}
