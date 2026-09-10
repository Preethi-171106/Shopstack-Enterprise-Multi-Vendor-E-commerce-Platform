package com.shopstack.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    /**
     * Optional coupon code to apply to this order.
     * The backend validates and calculates the discount — client totals are ignored.
     */
    private String couponCode;

}
