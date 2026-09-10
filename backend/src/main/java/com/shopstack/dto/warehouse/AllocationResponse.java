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
public class AllocationResponse {
    private Long id;
    private Long orderItemId;
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String shippingAddress;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private BigDecimal unitPrice;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseCity;
    private int allocatedQuantity;
    private String allocationStatus;
    private LocalDateTime allocatedAt;
    private LocalDateTime pickedAt;
    private LocalDateTime packedAt;
    private LocalDateTime readyForShipmentAt;
    private String notes;
}
