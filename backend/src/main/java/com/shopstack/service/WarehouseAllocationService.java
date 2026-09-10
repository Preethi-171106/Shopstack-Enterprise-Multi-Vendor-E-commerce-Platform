package com.shopstack.service;

import com.shopstack.dto.warehouse.AllocationActionRequest;
import com.shopstack.dto.warehouse.AllocationResponse;
import com.shopstack.entity.Order;

import java.util.List;

public interface WarehouseAllocationService {

    List<AllocationResponse> allocateOrderItems(Order order);

    List<AllocationResponse> getAllocations(Long warehouseId, String status);

    List<AllocationResponse> getPickingAllocations(Long warehouseId);

    List<AllocationResponse> getPackingAllocations(Long warehouseId);

    List<AllocationResponse> getReadyToShipAllocations(Long warehouseId);

    AllocationResponse getAllocationById(Long allocationId);

    AllocationResponse pickAllocation(Long allocationId);

    AllocationResponse packAllocation(Long allocationId, AllocationActionRequest request);

    AllocationResponse readyForShipment(Long allocationId, AllocationActionRequest request);

    void releaseOrderAllocations(Long orderId);
}
