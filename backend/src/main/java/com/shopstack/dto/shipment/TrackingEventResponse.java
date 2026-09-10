package com.shopstack.dto.shipment;

import com.shopstack.entity.ShipmentStatus;

import java.time.LocalDateTime;

/**
 * TrackingEventResponse — DTO representing an individual tracking event in history.
 */
public record TrackingEventResponse(
        Long id,
        ShipmentStatus status,
        String location,
        String description,
        LocalDateTime timestamp
) {
}
