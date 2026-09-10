package com.shopstack.repository;

import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    long countByRole(UserRole role);

    long countByRoleAndCreatedAtBetween(UserRole role, LocalDateTime start, LocalDateTime end);

    @Query(value = """
            SELECT u.id AS customerId,
                   u.full_name AS customerName,
                   u.email AS email,
                   COALESCE(SUM(o.total_amount), 0) AS totalSpent,
                   COUNT(o.id) AS ordersCount
            FROM users u
            LEFT JOIN orders o ON o.user_id = u.id
                AND o.order_date BETWEEN :start AND :end
                AND o.status IN ('PAID','SHIPPED','DELIVERED')
            WHERE u.role = 'CUSTOMER'
            GROUP BY u.id, u.full_name, u.email
            ORDER BY totalSpent DESC
            LIMIT :limit
            """, nativeQuery = true)
    @QueryHints({@QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<CustomerSummaryProjection> topCustomers(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("limit") int limit);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.orderDate BETWEEN :start AND :end " +
           "AND o.status IN (com.shopstack.entity.OrderStatus.PAID, " +
           "com.shopstack.entity.OrderStatus.SHIPPED, com.shopstack.entity.OrderStatus.DELIVERED)")
    BigDecimal totalCustomerSpendingBetween(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
