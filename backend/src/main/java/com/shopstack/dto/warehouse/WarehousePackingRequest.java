package com.shopstack.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehousePackingRequest {
    private String packageWeight;
    private String packageDimensions;
    private String packingNotes;
}
