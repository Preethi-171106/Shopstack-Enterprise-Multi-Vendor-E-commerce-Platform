package com.shopstack.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationActionRequest {
    private String notes;
    private String packageWeight;
    private String packageDimensions;
    private String carrier;
    private LocalDateTime estimatedDeliveryDate;
}
