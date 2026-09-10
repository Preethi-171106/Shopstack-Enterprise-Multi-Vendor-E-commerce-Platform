package com.shopstack.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseOrderResponse {
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerEmail;
    private String customerFullName;
    private String customerPhone;
    private String shippingAddress;
    private String orderStatus;
    private String warehouseStatus; // READY_FOR_PICKING, PICKING, PICKED, PACKING, PACKED, READY_TO_SHIP, SHIPPED
    private BigDecimal totalAmount;
    private int totalItems;
    private List<WarehouseOrderItemResponse> items;
    private Long shipmentId;
    private String trackingNumber;
    private String carrier;
    private String shipmentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
