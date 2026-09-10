package com.shopstack.repository;

import com.shopstack.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ShipmentRepository — Spring Data JPA repository for {@link Shipment} entity.
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    Optional<Shipment> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    boolean existsByTrackingNumber(String trackingNumber);

    List<Shipment> findByOrderUserId(Long userId);

    @Query("SELECT DISTINCT s FROM Shipment s JOIN s.order o JOIN o.items item WHERE item.vendorProfile.id = :vendorProfileId")
    List<Shipment> findDistinctByVendorProfileId(@Param("vendorProfileId") Long vendorProfileId);
}
