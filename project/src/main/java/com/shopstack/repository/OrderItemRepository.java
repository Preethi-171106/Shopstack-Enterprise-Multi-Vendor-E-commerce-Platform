package com.shopstack.repository;

import com.shopstack.entity.OrderItem;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query(value = """
            SELECT oi.product_id AS productId,
                   oi.product_name AS productName,
                   COALESCE(SUM(oi.quantity), 0) AS quantitySold,
                   COALESCE(SUM(oi.subtotal), 0) AS revenue
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.order_date BETWEEN :start AND :end
              AND (:vendorId IS NULL OR o.vendor_id = :vendorId)
              AND (:categoryId IS NULL
                   OR EXISTS (SELECT 1 FROM products p WHERE p.id = oi.product_id AND p.category_id = :categoryId))
            GROUP BY oi.product_id, oi.product_name
            ORDER BY quantitySold DESC
            """, nativeQuery = true)
    @QueryHints({@QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<ProductRevenueProjection> productRevenue(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  @Param("vendorId") Long vendorId,
                                                  @Param("categoryId") Long categoryId);
}
