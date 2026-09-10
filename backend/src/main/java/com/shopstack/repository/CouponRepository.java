package com.shopstack.repository;

import com.shopstack.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT c FROM Coupon c WHERE c.active = true AND c.startDate <= :now AND c.expiryDate >= :now ORDER BY c.createdAt DESC")
    List<Coupon> findActiveCoupons(@Param("now") LocalDateTime now);

    List<Coupon> findAllByOrderByCreatedAtDesc();

    long countByActiveTrue();

    long countByActiveFalse();

    @Query("SELECT c FROM Coupon c WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.createdAt DESC")
    List<Coupon> searchCoupons(@Param("search") String search);
}
