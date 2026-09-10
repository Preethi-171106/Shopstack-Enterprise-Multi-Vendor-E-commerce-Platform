package com.shopstack.dto.returns;

import com.shopstack.entity.QualityCheckResult;
import com.shopstack.entity.ReturnReason;
import com.shopstack.entity.ReturnStatus;
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
public class ReturnResponse {
    private Long id;
    private String returnNumber;
    private Long orderId;
    private String orderNumber;
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private int returnQuantity;
    private Long userId;
    private String userEmail;
    private String customerName;
    private Long originalWarehouseId;
    private String originalWarehouseCode;
    private String originalWarehouseName;
    private Long returnWarehouseId;
    private String returnWarehouseCode;
    private String returnWarehouseName;
    private ReturnReason reason;
    private String description;
    private String adminNotes;
    private ReturnStatus status;
    private BigDecimal refundAmount;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime returnReceivedDate;
    private String packageCondition;
    private String receivingNotes;
    private String qcStatus;
    private QualityCheckResult qcResult;
    private String qcNotes;
    private String qcInspectorName;
    private LocalDateTime qcCompletedAt;
    private int acceptedQuantity;
    private int damagedQuantity;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
