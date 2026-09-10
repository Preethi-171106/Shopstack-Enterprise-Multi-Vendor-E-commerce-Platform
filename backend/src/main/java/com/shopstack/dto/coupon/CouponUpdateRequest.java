package com.shopstack.dto.coupon;

import com.shopstack.entity.CouponApplicabilityScope;
import com.shopstack.entity.CouponDiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUpdateRequest {

    @NotBlank(message = "Coupon name is required")
    private String name;

    private String description;

    @NotNull(message = "Discount type is required")
    private CouponDiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
    private BigDecimal minimumOrderAmount;

    @DecimalMin(value = "0.01", message = "Maximum discount must be greater than 0")
    private BigDecimal maximumDiscount;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @Min(value = 1, message = "Per-user limit must be at least 1")
    private Integer perUserLimit;

    @Builder.Default
    private CouponApplicabilityScope applicabilityScope = CouponApplicabilityScope.ENTIRE_PLATFORM;

    private Set<Long> categoryIds;

    private Set<Long> productIds;

    private Set<Long> vendorIds;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiryDate;
}
