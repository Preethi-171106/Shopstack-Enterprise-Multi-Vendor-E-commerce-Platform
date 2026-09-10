package com.shopstack.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorRevenueResponse {

    private Long vendorProfileId;
    private String vendorStoreName;
    private BigDecimal totalSales;
    private BigDecimal totalCommission;
    private BigDecimal totalNetRevenue;
    private long orderCount;
}
