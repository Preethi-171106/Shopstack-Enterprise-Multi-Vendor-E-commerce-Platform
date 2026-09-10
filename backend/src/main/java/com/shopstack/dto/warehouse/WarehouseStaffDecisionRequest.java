package com.shopstack.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WarehouseStaffDecisionRequest — Optional payload when rejecting or assigning warehouse staff.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseStaffDecisionRequest {
    private String reason;
    private Long warehouseId;
}
