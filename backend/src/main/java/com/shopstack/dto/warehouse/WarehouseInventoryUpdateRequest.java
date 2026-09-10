package com.shopstack.dto.warehouse;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseInventoryUpdateRequest {

    /**
     * Adjustment type: 'ADD' | 'REMOVE' | 'SET' | 'DAMAGE' | 'CORRECTION'
     */
    private String adjustmentType;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    private String reason;
}
