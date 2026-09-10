package com.shopstack.dto.coupon;

import com.shopstack.entity.CouponApplicabilityScope;
import com.shopstack.entity.CouponDiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private CouponDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscount;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer perUserLimit;
    private CouponApplicabilityScope applicabilityScope;
    private Set<Long> eligibleCategoryIds;
    private Set<Long> eligibleProductIds;
    private Set<Long> eligibleVendorIds;
    private List<String> eligibleCategoryNames;
    private List<String> eligibleProductNames;
    private List<String> eligibleVendorNames;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private Boolean active;
    private String status; // Derived status (ACTIVE, EXPIRED, DISABLED)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
