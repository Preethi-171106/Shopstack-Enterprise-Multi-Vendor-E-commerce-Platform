package com.shopstack.repository;

import com.shopstack.entity.WarehouseStaffProfile;
import com.shopstack.entity.WarehouseStaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * WarehouseStaffProfileRepository — Spring Data JPA repository for {@link WarehouseStaffProfile}.
 */
@Repository
public interface WarehouseStaffProfileRepository extends JpaRepository<WarehouseStaffProfile, Long> {

    Optional<WarehouseStaffProfile> findByUserId(Long userId);

    @Query("SELECT wsp FROM WarehouseStaffProfile wsp JOIN FETCH wsp.user u LEFT JOIN FETCH wsp.warehouse w WHERE LOWER(u.email) = LOWER(:email)")
    Optional<WarehouseStaffProfile> findByUserEmailIgnoreCase(@Param("email") String email);

    List<WarehouseStaffProfile> findByStatus(WarehouseStaffStatus status);

    List<WarehouseStaffProfile> findByWarehouseId(Long warehouseId);

    @Query("SELECT wsp FROM WarehouseStaffProfile wsp JOIN FETCH wsp.user u LEFT JOIN FETCH wsp.warehouse w ORDER BY wsp.createdAt DESC")
    List<WarehouseStaffProfile> findAllWithDetails();
}
