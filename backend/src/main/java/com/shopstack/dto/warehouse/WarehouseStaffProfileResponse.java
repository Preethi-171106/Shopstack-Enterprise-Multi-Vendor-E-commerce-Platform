package com.shopstack.dto.warehouse;

import com.shopstack.entity.WarehouseStaffStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WarehouseStaffProfileResponse — Safe DTO returned for staff profile inspection and admin management.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseStaffProfileResponse {

    private Long id;
    private Long userId;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private WarehouseStaffStatus status;
    private String rejectionReason;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
