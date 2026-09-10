package com.shopstack.service;

import com.shopstack.dto.warehouse.WarehouseCreateRequest;
import com.shopstack.dto.warehouse.WarehouseInventoryCreateRequest;
import com.shopstack.dto.warehouse.WarehouseInventoryResponse;
import com.shopstack.dto.warehouse.WarehouseInventoryUpdateRequest;
import com.shopstack.dto.warehouse.WarehouseResponse;
import com.shopstack.dto.warehouse.WarehouseUpdateRequest;

import java.util.List;

public interface WarehouseManagementService {

    List<WarehouseResponse> getAllWarehouses(String search);

    List<WarehouseResponse> getActiveWarehouses();

    WarehouseResponse getWarehouseById(Long id);

    WarehouseResponse createWarehouse(WarehouseCreateRequest request);

    WarehouseResponse updateWarehouse(Long id, WarehouseUpdateRequest request);

    WarehouseResponse activateWarehouse(Long id);

    WarehouseResponse deactivateWarehouse(Long id);

    List<WarehouseInventoryResponse> getWarehouseInventories(Long warehouseId);

    List<WarehouseInventoryResponse> getAllWarehouseInventories();

    WarehouseInventoryResponse addOrUpdateProductStock(Long warehouseId, WarehouseInventoryCreateRequest request);

    WarehouseInventoryResponse adjustProductStock(Long warehouseId, Long productId, WarehouseInventoryUpdateRequest request);

    com.shopstack.dto.warehouse.StockDistributionResponse distributeStock(com.shopstack.dto.warehouse.StockDistributionRequest request);

    com.shopstack.dto.warehouse.StockDistributionResponse getDistributionOverview(Long productId);

    List<WarehouseInventoryResponse> getDamagedAndQuarantineInventories(Long warehouseId);
}
