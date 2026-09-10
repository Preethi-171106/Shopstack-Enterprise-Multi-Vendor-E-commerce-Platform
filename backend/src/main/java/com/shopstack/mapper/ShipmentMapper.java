package com.shopstack.mapper;

import com.shopstack.dto.shipment.ShipmentResponse;
import com.shopstack.dto.shipment.TrackingEventResponse;
import com.shopstack.entity.Shipment;
import com.shopstack.entity.TrackingEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ShipmentMapper — Maps {@link Shipment} and {@link TrackingEvent} entities to DTOs.
 */
@Component
public class ShipmentMapper {

    public TrackingEventResponse toTrackingEventResponse(TrackingEvent event) {
        if (event == null) return null;
        return new TrackingEventResponse(
                event.getId(),
                event.getStatus(),
                event.getLocation(),
                event.getDescription(),
                event.getTimestamp()
        );
    }

    public ShipmentResponse toShipmentResponse(Shipment shipment) {
        if (shipment == null) return null;

        List<TrackingEventResponse> trackingHistory = shipment.getTrackingEvents() == null
                ? List.of()
                : shipment.getTrackingEvents().stream()
                        .map(this::toTrackingEventResponse)
                        .toList();

        return new ShipmentResponse(
                shipment.getId(),
                shipment.getTrackingNumber(),
                shipment.getOrder().getId(),
                shipment.getOrder().getOrderNumber(),
                shipment.getCarrier(),
                shipment.getStatus(),
                shipment.getShippingAddress(),
                shipment.getEstimatedDeliveryDate(),
                shipment.getShippedDate(),
                shipment.getDeliveredDate(),
                trackingHistory,
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }
}
