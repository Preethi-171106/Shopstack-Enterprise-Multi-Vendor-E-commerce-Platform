package com.shopstack.mapper;

import com.shopstack.dto.warehouse.WarehouseInventoryResponse;
import com.shopstack.entity.Product;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseInventory;
import org.springframework.stereotype.Component;

@Component
public class WarehouseInventoryMapper {

    public WarehouseInventoryResponse toResponse(WarehouseInventory wi) {
        if (wi == null) return null;

        Warehouse w = wi.getWarehouse();
        Product p = wi.getProduct();

        return WarehouseInventoryResponse.builder()
                .id(wi.getId())
                .warehouseId(w != null ? w.getId() : null)
                .warehouseCode(w != null ? w.getWarehouseCode() : null)
                .warehouseName(w != null ? w.getName() : null)
                .productId(p != null ? p.getId() : null)
                .productName(p != null ? p.getName() : null)
                .productSku(p != null ? p.getSku() : null)
                .productImageUrl(p != null ? p.getImageUrl() : null)
                .productPrice(p != null ? p.getPrice() : null)
                .totalQuantity(wi.getTotalQuantity())
                .reservedQuantity(wi.getReservedQuantity())
                .availableQuantity(wi.getAvailableQuantity())
                .lowStockThreshold(wi.getLowStockThreshold())
                .damagedQuantity(wi.getDamagedQuantity())
                .quarantineQuantity(wi.getQuarantineQuantity())
                .lowStock(wi.isLowStock())
                .outOfStock(wi.getAvailableQuantity() <= 0 || wi.getTotalQuantity() <= 0)
                .updatedAt(wi.getUpdatedAt())
                .build();
    }
}
