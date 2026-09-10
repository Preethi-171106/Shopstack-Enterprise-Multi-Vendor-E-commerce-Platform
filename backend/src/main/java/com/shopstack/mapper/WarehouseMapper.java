package com.shopstack.mapper;

import com.shopstack.dto.warehouse.WarehouseCreateRequest;
import com.shopstack.dto.warehouse.WarehouseResponse;
import com.shopstack.dto.warehouse.WarehouseUpdateRequest;
import com.shopstack.entity.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class WarehouseMapper {

    public Warehouse toEntity(WarehouseCreateRequest request) {
        if (request == null) return null;
        return Warehouse.builder()
                .warehouseCode(request.getWarehouseCode() != null ? request.getWarehouseCode().trim().toUpperCase() : null)
                .name(request.getName() != null ? request.getName().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .city(request.getCity() != null ? request.getCity().trim() : null)
                .state(request.getState() != null ? request.getState().trim() : null)
                .postalCode(request.getPostalCode() != null ? request.getPostalCode().trim() : null)
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry().trim() : "India")
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
    }

    public void updateEntityFromRequest(Warehouse warehouse, WarehouseUpdateRequest request) {
        if (warehouse == null || request == null) return;
        if (request.getName() != null) warehouse.setName(request.getName().trim());
        if (request.getAddress() != null) warehouse.setAddress(request.getAddress().trim());
        if (request.getCity() != null) warehouse.setCity(request.getCity().trim());
        if (request.getState() != null) warehouse.setState(request.getState().trim());
        if (request.getPostalCode() != null) warehouse.setPostalCode(request.getPostalCode().trim());
        if (request.getCountry() != null && !request.getCountry().isBlank()) warehouse.setCountry(request.getCountry().trim());
        if (request.getActive() != null) warehouse.setActive(request.getActive());
    }

    public WarehouseResponse toResponse(Warehouse warehouse) {
        return toResponse(warehouse, null, null);
    }

    public WarehouseResponse toResponse(Warehouse warehouse, Integer totalSkus, Long totalStock) {
        if (warehouse == null) return null;
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .warehouseCode(warehouse.getWarehouseCode())
                .name(warehouse.getName())
                .address(warehouse.getAddress())
                .city(warehouse.getCity())
                .state(warehouse.getState())
                .postalCode(warehouse.getPostalCode())
                .country(warehouse.getCountry())
                .active(warehouse.getActive())
                .totalSkus(totalSkus)
                .totalStock(totalStock)
                .createdAt(warehouse.getCreatedAt())
                .updatedAt(warehouse.getUpdatedAt())
                .build();
    }
}
