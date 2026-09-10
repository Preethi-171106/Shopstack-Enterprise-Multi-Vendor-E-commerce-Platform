package com.shopstack.repository;

import com.shopstack.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TrackingEventRepository — Spring Data JPA repository for {@link TrackingEvent} entity.
 */
@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    List<TrackingEvent> findByShipmentIdOrderByTimestampAsc(Long shipmentId);
}
