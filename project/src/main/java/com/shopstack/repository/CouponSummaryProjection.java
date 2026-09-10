package com.shopstack.repository;

import java.math.BigDecimal;

public interface CouponSummaryProjection {
    Long getCouponId();
    String getCode();
    Long getUsageCount();
    BigDecimal getTotalDiscount();
    BigDecimal getDiscountPercentage();
}
