package com.shopstack.order.repository;

import com.shopstack.order.entity.Order;
import com.shopstack.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    long countByStatus(OrderStatus status);

    long countByVendorId(Long vendorId);

    long countByVendorIdAndStatus(Long vendorId, OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end")
    long countByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT SUM(o.totalAmount) FROM Order o
            WHERE o.status IN (
                com.shopstack.order.enums.OrderStatus.DELIVERED,
                com.shopstack.order.enums.OrderStatus.CONFIRMED,
                com.shopstack.order.enums.OrderStatus.SHIPPED
            )
            """)
    BigDecimal sumTotalRevenue();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.vendor.id = :vendorId AND o.status = com.shopstack.order.enums.OrderStatus.DELIVERED")
    BigDecimal sumRevenueByVendor(@Param("vendorId") Long vendorId);

    @Query("""
            SELECT SUM(o.totalAmount) FROM Order o
            WHERE o.vendor.id = :vendorId AND o.status = com.shopstack.order.enums.OrderStatus.DELIVERED
            AND o.createdAt >= :start AND o.createdAt < :end
            """)
    BigDecimal sumRevenueByVendorAndDateRange(@Param("vendorId") Long vendorId,
                                               @Param("start") Instant start,
                                               @Param("end") Instant end);

    @Query("""
            SELECT SUM(o.totalAmount) FROM Order o
            WHERE o.status IN (
                com.shopstack.order.enums.OrderStatus.DELIVERED,
                com.shopstack.order.enums.OrderStatus.CONFIRMED,
                com.shopstack.order.enums.OrderStatus.SHIPPED
            )
            AND o.createdAt >= :start AND o.createdAt < :end
            """)
    BigDecimal sumRevenueByDateRange(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT FUNCTION('DATE', o.createdAt), COUNT(o), SUM(o.totalAmount)
            FROM Order o
            WHERE o.createdAt >= :start AND o.createdAt < :end
            GROUP BY FUNCTION('DATE', o.createdAt)
            ORDER BY FUNCTION('DATE', o.createdAt)
            """)
    List<Object[]> findDailySalesStats(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT FUNCTION('DATE', o.createdAt), COUNT(o), SUM(o.totalAmount)
            FROM Order o
            WHERE o.vendor.id = :vendorId AND o.createdAt >= :start AND o.createdAt < :end
            GROUP BY FUNCTION('DATE', o.createdAt)
            ORDER BY FUNCTION('DATE', o.createdAt)
            """)
    List<Object[]> findDailySalesStatsByVendor(@Param("vendorId") Long vendorId,
                                                @Param("start") Instant start,
                                                @Param("end") Instant end);

    @Query("""
            SELECT o.status, COUNT(o)
            FROM Order o
            GROUP BY o.status
            """)
    List<Object[]> countGroupByStatus();

    @Query("""
            SELECT o.status, COUNT(o)
            FROM Order o
            WHERE o.vendor.id = :vendorId
            GROUP BY o.status
            """)
    List<Object[]> countGroupByStatusByVendor(@Param("vendorId") Long vendorId);

    @Query("""
            SELECT FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM'), COUNT(o), COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.createdAt >= :start AND o.createdAt < :end
            GROUP BY FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM')
            ORDER BY FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM')
            """)
    List<Object[]> findMonthlySalesStats(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM'), COUNT(o), COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.vendor.id = :vendorId AND o.createdAt >= :start AND o.createdAt < :end
            GROUP BY FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM')
            ORDER BY FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM')
            """)
    List<Object[]> findMonthlySalesStatsByVendor(@Param("vendorId") Long vendorId,
                                                  @Param("start") Instant start,
                                                  @Param("end") Instant end);

    @Query("""
            SELECT COUNT(oi) FROM OrderItem oi
            WHERE oi.order.customer.id = :userId
            AND oi.product.id = :productId
            AND oi.order.status NOT IN (
                com.shopstack.order.enums.OrderStatus.CANCELLED,
                com.shopstack.order.enums.OrderStatus.RETURNED
            )
            """)
    long countDeliveredOrderItemsByUserAndProduct(@Param("userId") Long userId,
                                                   @Param("productId") Long productId);
}
