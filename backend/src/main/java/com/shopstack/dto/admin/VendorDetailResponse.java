package com.shopstack.dto.admin;

import com.shopstack.dto.product.ProductResponse;
import com.shopstack.entity.VendorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * VendorDetailResponse — comprehensive vendor detail view for Admin management.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorDetailResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private String storeName;
    private String description;
    private String contactNumber;
    private String address;
    private String taxId;
    private VendorStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Aggregated metrics from live database
    private long totalProducts;
    private long activeProducts;
    private long totalOrdersCount;
    private BigDecimal totalGrossSales;
    private BigDecimal totalCommissionPaid;
    private BigDecimal totalNetEarnings;

    private List<ProductResponse> recentProducts;
}
