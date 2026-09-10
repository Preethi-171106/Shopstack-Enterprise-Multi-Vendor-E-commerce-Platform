package com.shopstack.repository;

import com.shopstack.entity.Commission;
import com.shopstack.entity.CommissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {

    List<Commission> findByVendorProfileId(Long vendorProfileId);

    List<Commission> findByVendorProfileIdAndStatus(Long vendorProfileId, CommissionStatus status);

    List<Commission> findByStatus(CommissionStatus status);

    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM Commission c WHERE c.status != 'VOIDED'")
    BigDecimal sumTotalCommissions();

    @Query("SELECT COALESCE(SUM(c.saleAmount), 0) FROM Commission c WHERE c.vendorProfile.id = :vendorId AND c.status != 'VOIDED'")
    BigDecimal sumSalesByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT COALESCE(SUM(c.vendorNetAmount), 0) FROM Commission c WHERE c.vendorProfile.id = :vendorId AND c.status != 'VOIDED'")
    BigDecimal sumVendorNetByVendor(@Param("vendorId") Long vendorId);

    long countByStatus(CommissionStatus status);
}
