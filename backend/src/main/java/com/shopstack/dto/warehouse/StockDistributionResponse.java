package com.shopstack.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDistributionResponse {
    private Long productId;
    private String productName;
    private String productSku;
    private int totalGlobalStock;
    private int totalWarehouseAllocatedStock;
    private int unallocatedCentralStock;
    private List<WarehouseInventoryResponse> warehouseInventories;

    public int getGlobalTotalStock() {
        return totalGlobalStock;
    }

    public int getAllocatedStock() {
        return totalWarehouseAllocatedStock;
    }

    public int getUnallocatedStock() {
        return unallocatedCentralStock;
    }

    public List<WarehouseInventoryResponse> getWarehouseAllocations() {
        return warehouseInventories;
    }
}
