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
public class WarehouseDashboardResponse {
    private long totalTrackedInventories;
    private long totalStockQuantity;
    private long lowStockCount;
    private long outOfStockCount;
    private long totalProcessingOrders;
    private long readyForPickingOrders;
    private long inPickingOrders;
    private long readyForPackingOrders;
    private long packedOrders;
    private long readyToShipShipments;
    private long activeShipments;
    private long deliveredToday;
    private long totalStockMovementsToday;
    private List<WarehouseOrderResponse> urgentOrders;
}

