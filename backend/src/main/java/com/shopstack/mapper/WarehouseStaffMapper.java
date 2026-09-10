package com.shopstack.mapper;

import com.shopstack.dto.warehouse.WarehouseStaffProfileResponse;
import com.shopstack.entity.User;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseStaffProfile;
import org.springframework.stereotype.Component;

/**
 * WarehouseStaffMapper — Transforms {@link WarehouseStaffProfile} to safe {@link WarehouseStaffProfileResponse}.
 */
@Component
public class WarehouseStaffMapper {

    public WarehouseStaffProfileResponse toResponse(WarehouseStaffProfile profile) {
        if (profile == null) {
            return null;
        }

        User user = profile.getUser();
        Warehouse warehouse = profile.getWarehouse();
        User approvedBy = profile.getApprovedBy();

        String fullName = user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : null;
        String approvedByName = approvedBy != null ? (approvedBy.getFirstName() + " " + approvedBy.getLastName()).trim() : null;

        return WarehouseStaffProfileResponse.builder()
                .id(profile.getId())
                .userId(user != null ? user.getId() : null)
                .name(fullName)
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
                .email(user != null ? user.getEmail() : null)
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .warehouseId(warehouse != null ? warehouse.getId() : null)
                .warehouseCode(warehouse != null ? warehouse.getWarehouseCode() : null)
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .status(profile.getStatus())
                .rejectionReason(profile.getRejectionReason())
                .approvedByName(approvedByName)
                .approvedAt(profile.getApprovedAt())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
