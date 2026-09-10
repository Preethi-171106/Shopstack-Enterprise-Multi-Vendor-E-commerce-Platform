package com.shopstack.service;

import com.shopstack.dto.warehouse.WarehouseDashboardResponse;
import com.shopstack.dto.warehouse.WarehouseOrderResponse;
import com.shopstack.dto.warehouse.WarehousePackingRequest;
import com.shopstack.dto.warehouse.WarehouseReadyToShipRequest;

import java.util.List;

public interface WarehouseService {

    WarehouseDashboardResponse getWarehouseDashboard();

    List<WarehouseOrderResponse> getWarehouseOrders(String statusFilter);

    List<WarehouseOrderResponse> getPickingOrders();

    List<WarehouseOrderResponse> getPackingOrders();

    List<WarehouseOrderResponse> getReadyToShipOrders();

    WarehouseOrderResponse getWarehouseOrderById(Long orderId);

    WarehouseOrderResponse startPicking(Long orderId);

    WarehouseOrderResponse markPicked(Long orderId);

    WarehouseOrderResponse startPacking(Long orderId);

    WarehouseOrderResponse markPacked(Long orderId, WarehousePackingRequest request);

    WarehouseOrderResponse readyToShip(Long orderId, WarehouseReadyToShipRequest request);
}

