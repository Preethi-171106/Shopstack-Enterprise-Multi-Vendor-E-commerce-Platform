package com.shopstack.dto.shipment;

import com.shopstack.entity.ShipmentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ShipmentResponse — Complete client view of shipment details and tracking history.
 */
public record ShipmentResponse(
        Long id,
        String trackingNumber,
        Long orderId,
        String orderNumber,
        String carrier,
        ShipmentStatus status,
        String shippingAddress,
        LocalDateTime estimatedDeliveryDate,
        LocalDateTime shippedDate,
        LocalDateTime deliveredDate,
        List<TrackingEventResponse> trackingHistory,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
