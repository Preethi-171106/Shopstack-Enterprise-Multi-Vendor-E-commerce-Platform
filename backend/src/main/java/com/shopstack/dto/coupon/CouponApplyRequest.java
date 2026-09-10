package com.shopstack.dto.coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponApplyRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Order total is required to calculate discount")
    @DecimalMin(value = "0.01", message = "Order total must be greater than 0")
    private BigDecimal orderTotal;

    /**
     * Optional: the authenticated user's email address.
     * When provided, per-user usage limit is validated.
     */
    private String userEmail;
}
