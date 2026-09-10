package com.shopstack.dto.warehouse;

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
public class WarehouseInventoryResponse {
    private Long id;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private BigDecimal productPrice;
    private int totalQuantity;
    private int reservedQuantity;
    private int availableQuantity;
    private int lowStockThreshold;
    private int damagedQuantity;
    private int quarantineQuantity;
    private boolean lowStock;
    private boolean outOfStock;
    private LocalDateTime updatedAt;
}
