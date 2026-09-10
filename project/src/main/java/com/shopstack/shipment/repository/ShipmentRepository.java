package com.shopstack.shipment.repository;

import com.shopstack.shipment.entity.Shipment;
import com.shopstack.shipment.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    long countByStatus(ShipmentStatus status);

    @Query("SELECT s.status, COUNT(s) FROM Shipment s GROUP BY s.status")
    List<Object[]> countGroupByStatus();
}
