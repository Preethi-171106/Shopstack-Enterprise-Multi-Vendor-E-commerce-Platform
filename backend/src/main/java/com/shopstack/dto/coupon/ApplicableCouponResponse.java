package com.shopstack.dto.coupon;

import com.shopstack.entity.CouponApplicabilityScope;
import com.shopstack.entity.CouponDiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicableCouponResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private CouponDiscountType discountType;
    private BigDecimal discountValue;
    private CouponApplicabilityScope applicabilityScope;
    private BigDecimal eligibleSubtotal;
    private BigDecimal estimatedDiscount;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscount;
    private String message;
    private Set<Long> eligibleCategoryIds;
    private Set<Long> eligibleProductIds;
    private Set<Long> eligibleVendorIds;
}
