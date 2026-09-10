package com.shopstack.repository;

import com.shopstack.entity.Coupon;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query(value = """
            SELECT c.id AS couponId,
                   c.code AS code,
                   COALESCE(c.usage_count, 0) AS usageCount,
                   COALESCE(c.total_discount, 0) AS totalDiscount,
                   c.discount_percentage AS discountPercentage
            FROM coupons c
            WHERE EXISTS (
                SELECT 1 FROM orders o
                WHERE o.coupon_id = c.id
                  AND o.order_date BETWEEN :start AND :end
            )
            ORDER BY usageCount DESC
            """, nativeQuery = true)
    @QueryHints({@QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<CouponSummaryProjection> couponUsageBetween(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(c.totalDiscount), 0) FROM Coupon c")
    BigDecimal totalDiscountAll();

    @Query("SELECT COALESCE(SUM(c.usageCount), 0) FROM Coupon c")
    Long totalUsageAll();
}
