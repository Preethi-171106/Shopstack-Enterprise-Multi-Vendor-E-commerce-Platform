package com.shopstack.repository;

import com.shopstack.entity.Coupon;
import com.shopstack.entity.CouponUsage;
import com.shopstack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    int countByCoupon(Coupon coupon);

    int countByCouponAndUser(Coupon coupon, User user);

    List<CouponUsage> findByUserId(Long userId);

    List<CouponUsage> findByCouponIdOrderByUsedAtDesc(Long couponId);

    List<CouponUsage> findByCouponOrderByUsedAtDesc(Coupon coupon);

    @Query("SELECT COALESCE(SUM(u.discountAmount), 0) FROM CouponUsage u")
    BigDecimal sumTotalDiscountAmount();

    @Query("SELECT COALESCE(SUM(u.discountAmount), 0) FROM CouponUsage u WHERE u.coupon.id = :couponId")
    BigDecimal sumDiscountAmountByCouponId(@Param("couponId") Long couponId);

    @Query("SELECT COUNT(u) FROM CouponUsage u")
    long countTotalUsages();
}

