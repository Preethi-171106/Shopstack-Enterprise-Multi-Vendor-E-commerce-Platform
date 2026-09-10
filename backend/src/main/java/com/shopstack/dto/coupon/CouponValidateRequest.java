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
public class CouponValidateRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Order total is required")
    @DecimalMin(value = "0.01", message = "Order total must be greater than zero")
    private BigDecimal orderTotal;

    /**
     * Optional email of the user attempting to apply the coupon.
     * If not provided in the request body, authenticated user's email is used.
     */
    private String userEmail;
}
