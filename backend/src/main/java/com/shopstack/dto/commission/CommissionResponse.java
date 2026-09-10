package com.shopstack.dto.commission;

import com.shopstack.entity.CommissionStatus;
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
public class CommissionResponse {

    private Long id;
    private Long vendorProfileId;
    private String vendorStoreName;
    private Long orderItemId;
    private String productName;
    private BigDecimal saleAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal vendorNetAmount;
    private CommissionStatus status;
    private LocalDateTime createdAt;
}
