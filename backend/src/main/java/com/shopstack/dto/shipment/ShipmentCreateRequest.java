package com.shopstack.dto.shipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * ShipmentCreateRequest — DTO for registering a new shipment for an order.
 */
public record ShipmentCreateRequest(

        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotBlank(message = "Carrier name is required")
        String carrier,

        @NotBlank(message = "Shipping address is required")
        String shippingAddress,

        LocalDateTime estimatedDeliveryDate,

        @NotBlank(message = "Initial location is required")
        String location,

        String description
) {
}
