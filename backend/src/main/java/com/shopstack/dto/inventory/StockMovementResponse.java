package com.shopstack.dto.inventory;

import com.shopstack.entity.StockMovementType;

import java.time.LocalDateTime;

/**
 * StockMovementResponse — Response DTO representing an immutable stock movement audit log entry.
 */
public record StockMovementResponse(
        Long id,
        Long productId,
        String productName,
        String productSku,
        StockMovementType movementType,
        int quantity,
        int previousStock,
        int newStock,
        String referenceId,
        String notes,
        Long performedByUserId,
        String performedByUserEmail,
        LocalDateTime createdAt
) {}
