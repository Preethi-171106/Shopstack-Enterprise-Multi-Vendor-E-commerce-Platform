package com.shopstack.repository;

import com.shopstack.entity.Order;
import com.shopstack.entity.OrderStatus;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate BETWEEN :start AND :end")
    Long countOrdersBetween(@Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.orderDate BETWEEN :start AND :end")
    Long countByStatusBetween(@Param("status") OrderStatus status,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :start AND :end")
    BigDecimal averageOrderValueBetween(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.vendorId = :vendorId AND o.orderDate BETWEEN :start AND :end")
    Long countByVendorBetween(@Param("vendorId") Long vendorId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.vendorId = :vendorId AND o.orderDate BETWEEN :start AND :end")
    BigDecimal sumRevenueByVendorBetween(@Param("vendorId") Long vendorId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.vendorId = :vendorId AND o.status = :status AND o.orderDate BETWEEN :start AND :end")
    Long countByVendorAndStatusBetween(@Param("vendorId") Long vendorId,
                                       @Param("status") OrderStatus status,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.vendorId = :vendorId AND o.orderDate BETWEEN :start AND :end")
    BigDecimal avgOrderValueByVendorBetween(@Param("vendorId") Long vendorId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT to_char(o.order_date, :fmt) AS periodLabel,
                   COALESCE(SUM(o.total_amount), 0) AS revenue,
                   COUNT(o.id) AS ordersCount
            FROM orders o
            WHERE o.order_date BETWEEN :start AND :end
              AND (:status IS NULL OR o.status = :status)
              AND (:vendorId IS NULL OR o.vendor_id = :vendorId)
            GROUP BY periodLabel
            ORDER BY periodLabel
            """, nativeQuery = true)
    @QueryHints({@QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<RevenueBreakdownProjection> revenueBreakdown(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end,
                                                       @Param("fmt") String fmt,
                                                       @Param("status") String status,
                                                       @Param("vendorId") Long vendorId);

    @Query(value = """
            SELECT o.status AS status, COUNT(o.id) AS count
            FROM orders o
            WHERE o.order_date BETWEEN :start AND :end
            GROUP BY o.status
            """, nativeQuery = true)
    List<StatusCountProjection> statusCounts(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
}
