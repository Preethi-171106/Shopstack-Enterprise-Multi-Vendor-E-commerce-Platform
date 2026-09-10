package com.shopstack.mapper;

import com.shopstack.dto.coupon.CouponUsageResponse;
import com.shopstack.entity.CouponUsage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CouponUsageMapper {

    public CouponUsageResponse toResponse(CouponUsage usage) {
        if (usage == null) {
            return null;
        }

        return CouponUsageResponse.builder()
                .id(usage.getId())
                .couponId(usage.getCoupon().getId())
                .couponCode(usage.getCoupon().getCode())
                .userId(usage.getUser().getId())
                .userEmail(usage.getUser().getEmail())
                .orderId(usage.getOrder().getId())
                .orderNumber(usage.getOrder().getOrderNumber())
                .discountAmount(usage.getDiscountAmount())
                .usedAt(usage.getUsedAt())
                .build();
    }

    public List<CouponUsageResponse> toResponseList(List<CouponUsage> usages) {
        if (usages == null) {
            return null;
        }
        return usages.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
