package com.shopstack.repository;

import com.shopstack.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByWarehouseCodeIgnoreCase(String warehouseCode);

    boolean existsByWarehouseCodeIgnoreCase(String warehouseCode);

    List<Warehouse> findByActiveTrue();

    @Query("SELECT w FROM Warehouse w WHERE " +
           "LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.state) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Warehouse> searchWarehouses(@Param("search") String search);
}
