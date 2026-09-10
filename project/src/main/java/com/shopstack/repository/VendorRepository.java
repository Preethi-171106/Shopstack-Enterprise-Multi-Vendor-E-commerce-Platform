package com.shopstack.repository;

import com.shopstack.entity.Vendor;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByUserId(Long userId);

    @Query(value = """
            SELECT v.id AS vendorId,
                   v.name AS vendorName,
                   COALESCE(SUM(o.total_amount), 0) AS revenue,
                   COUNT(DISTINCT o.id) AS ordersCount,
                   (SELECT COUNT(p.id) FROM products p WHERE p.vendor_id = v.id) AS productsCount
            FROM vendors v
            LEFT JOIN orders o ON o.vendor_id = v.id
                AND o.order_date BETWEEN :start AND :end
                AND o.status IN ('PAID','SHIPPED','DELIVERED')
            WHERE (:vendorId IS NULL OR v.id = :vendorId)
            GROUP BY v.id, v.name
            ORDER BY revenue DESC
            """, nativeQuery = true)
    @QueryHints({@QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<VendorSummaryProjection> vendorSummaries(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  @Param("vendorId") Long vendorId);

    @Query(value = """
            SELECT v.id AS vendorId,
                   v.name AS vendorName,
                   COALESCE(SUM(o.total_amount), 0) AS revenue,
                   COUNT(DISTINCT o.id) AS ordersCount,
                   (SELECT COUNT(p.id) FROM products p WHERE p.vendor_id = v.id) AS productsCount
            FROM vendors v
            LEFT JOIN orders o ON o.vendor_id = v.id
                AND o.order_date BETWEEN :start AND :end
                AND o.status IN ('PAID','SHIPPED','DELIVERED')
            GROUP BY v.id, v.name
            ORDER BY revenue DESC
            LIMIT :limit
            """, nativeQuery = true)
    @QueryHints({@QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<VendorSummaryProjection> topVendors(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             @Param("limit") int limit);
}
