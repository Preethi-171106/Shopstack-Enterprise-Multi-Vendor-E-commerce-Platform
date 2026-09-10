package com.shopstack.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseReadyToShipRequest {
    @NotBlank(message = "Carrier name is required")
    private String carrier;

    private String trackingNumber;

    private LocalDateTime estimatedDeliveryDate;

    private String shippingNotes;
}
