package com.shopstack.mapper;

import com.shopstack.dto.coupon.ApplicableCouponResponse;
import com.shopstack.dto.coupon.CouponResponse;
import com.shopstack.entity.Coupon;
import com.shopstack.entity.CouponApplicabilityScope;
import com.shopstack.entity.CouponStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CouponMapper {

    public CouponResponse toResponse(Coupon coupon) {
        if (coupon == null) {
            return null;
        }

        Set<Long> categoryIds = coupon.getEligibleCategories() != null
                ? coupon.getEligibleCategories().stream().map(c -> c.getId()).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<Long> productIds = coupon.getEligibleProducts() != null
                ? coupon.getEligibleProducts().stream().map(p -> p.getId()).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<Long> vendorIds = coupon.getEligibleVendors() != null
                ? coupon.getEligibleVendors().stream().map(v -> v.getId()).collect(Collectors.toSet())
                : Collections.emptySet();

        List<String> categoryNames = coupon.getEligibleCategories() != null
                ? coupon.getEligibleCategories().stream().map(c -> c.getName()).collect(Collectors.toList())
                : Collections.emptyList();

        List<String> productNames = coupon.getEligibleProducts() != null
                ? coupon.getEligibleProducts().stream().map(p -> p.getName()).collect(Collectors.toList())
                : Collections.emptyList();

        List<String> vendorNames = coupon.getEligibleVendors() != null
                ? coupon.getEligibleVendors().stream().map(v -> v.getStoreName()).collect(Collectors.toList())
                : Collections.emptyList();

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .perUserLimit(coupon.getPerUserLimit())
                .applicabilityScope(coupon.getApplicabilityScope() != null ? coupon.getApplicabilityScope() : CouponApplicabilityScope.ENTIRE_PLATFORM)
                .eligibleCategoryIds(categoryIds)
                .eligibleProductIds(productIds)
                .eligibleVendorIds(vendorIds)
                .eligibleCategoryNames(categoryNames)
                .eligibleProductNames(productNames)
                .eligibleVendorNames(vendorNames)
                .startDate(coupon.getStartDate())
                .expiryDate(coupon.getExpiryDate())
                .active(coupon.getActive())
                .status(calculateStatus(coupon).name())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }

    public ApplicableCouponResponse toApplicableResponse(Coupon coupon, BigDecimal eligibleSubtotal, BigDecimal estimatedDiscount, String message) {
        if (coupon == null) {
            return null;
        }

        Set<Long> categoryIds = coupon.getEligibleCategories() != null
                ? coupon.getEligibleCategories().stream().map(c -> c.getId()).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<Long> productIds = coupon.getEligibleProducts() != null
                ? coupon.getEligibleProducts().stream().map(p -> p.getId()).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<Long> vendorIds = coupon.getEligibleVendors() != null
                ? coupon.getEligibleVendors().stream().map(v -> v.getId()).collect(Collectors.toSet())
                : Collections.emptySet();

        return ApplicableCouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .applicabilityScope(coupon.getApplicabilityScope() != null ? coupon.getApplicabilityScope() : CouponApplicabilityScope.ENTIRE_PLATFORM)
                .eligibleSubtotal(eligibleSubtotal)
                .estimatedDiscount(estimatedDiscount)
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .message(message)
                .eligibleCategoryIds(categoryIds)
                .eligibleProductIds(productIds)
                .eligibleVendorIds(vendorIds)
                .build();
    }

    public List<CouponResponse> toResponseList(List<Coupon> coupons) {
        if (coupons == null) {
            return null;
        }
        return coupons.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CouponStatus calculateStatus(Coupon coupon) {
        if (Boolean.FALSE.equals(coupon.getActive())) {
            return CouponStatus.DISABLED;
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
            return CouponStatus.EXPIRED;
        }
        return CouponStatus.ACTIVE;
    }
}
