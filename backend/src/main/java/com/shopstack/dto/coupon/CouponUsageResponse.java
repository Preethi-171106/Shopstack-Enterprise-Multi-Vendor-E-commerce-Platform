package com.shopstack.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsageResponse {
    private Long id;
    private Long couponId;
    private String couponCode;
    private Long userId;
    private String userEmail;
    private Long orderId;
    private String orderNumber;
    private BigDecimal discountAmount;
    private LocalDateTime usedAt;
}
