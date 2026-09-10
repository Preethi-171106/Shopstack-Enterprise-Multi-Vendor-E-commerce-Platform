package com.shopstack.mapper;

import com.shopstack.dto.inventory.InventoryResponse;
import com.shopstack.entity.Inventory;
import org.springframework.stereotype.Component;

/**
 * InventoryMapper — Maps {@link Inventory} entities to {@link InventoryResponse} DTOs.
 */
@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        Long productId = inventory.getProduct() != null ? inventory.getProduct().getId() : null;
        String productName = inventory.getProduct() != null ? inventory.getProduct().getName() : null;
        String productSku = inventory.getProduct() != null ? inventory.getProduct().getSku() : null;
        
        Long vendorProfileId = (inventory.getProduct() != null && inventory.getProduct().getVendorProfile() != null)
                ? inventory.getProduct().getVendorProfile().getId() : null;
        String storeName = (inventory.getProduct() != null && inventory.getProduct().getVendorProfile() != null)
                ? inventory.getProduct().getVendorProfile().getStoreName() : null;

        return new InventoryResponse(
                inventory.getId(),
                productId,
                productName,
                productSku,
                vendorProfileId,
                storeName,
                inventory.getTotalStock(),
                inventory.getReservedStock(),
                inventory.getAvailableStock(),
                inventory.getLowStockThreshold(),
                inventory.isLowStock(),
                inventory.getVersion(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}
