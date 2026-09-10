package com.shopstack.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseOrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private int quantity; // Exact purchased quantity
    private BigDecimal unitPrice;
    private Long vendorProfileId;
    private String vendorStoreName;
    private int availableStock;
    private int totalStock;
    private boolean inStock;
    private String pickingStatus; // PENDING, PICKED
}
