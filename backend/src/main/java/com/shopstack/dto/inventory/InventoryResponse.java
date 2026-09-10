package com.shopstack.dto.inventory;

import java.time.LocalDateTime;

/**
 * InventoryResponse — Response DTO representing an inventory record.
 */
public record InventoryResponse(
        Long id,
        Long productId,
        String productName,
        String productSku,
        Long vendorProfileId,
        String storeName,
        int totalStock,
        int reservedStock,
        int availableStock,
        int lowStockThreshold,
        boolean isLowStock,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
