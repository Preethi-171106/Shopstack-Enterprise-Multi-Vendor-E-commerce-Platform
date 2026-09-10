package com.shopstack.dto.coupon;

import com.shopstack.entity.CouponApplicabilityScope;
import com.shopstack.entity.CouponDiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidateResponse {

    private boolean valid;
    private String code;
    private String name;
    private String description;
    private CouponDiscountType discountType;
    private BigDecimal discountValue;
    private CouponApplicabilityScope applicabilityScope;
    private BigDecimal eligibleSubtotal;
    private BigDecimal discountAmount;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscount;
    private BigDecimal finalTotal;
    private String message;
}
