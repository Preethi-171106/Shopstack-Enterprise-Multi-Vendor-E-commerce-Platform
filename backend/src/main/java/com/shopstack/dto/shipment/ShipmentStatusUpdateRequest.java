package com.shopstack.dto.shipment;

import com.shopstack.entity.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * ShipmentStatusUpdateRequest — DTO for updating shipment status and appending tracking event.
 */
public record ShipmentStatusUpdateRequest(

        @NotNull(message = "Shipment status is required")
        ShipmentStatus status,

        @NotBlank(message = "Current location is required")
        String location,

        String description
) {
}
