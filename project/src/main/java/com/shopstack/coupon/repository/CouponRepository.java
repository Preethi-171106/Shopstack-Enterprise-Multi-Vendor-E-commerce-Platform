package com.shopstack.coupon.repository;

import com.shopstack.coupon.entity.Coupon;
import com.shopstack.coupon.enums.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    long countByStatus(CouponStatus status);

    @Query("""
            SELECT c.code, c.description, c.discountType, c.discountValue, c.status, c.usedCount, c.maxUses
            FROM Coupon c
            ORDER BY c.usedCount DESC
            """)
    List<Object[]> findCouponUsageStats();
}
