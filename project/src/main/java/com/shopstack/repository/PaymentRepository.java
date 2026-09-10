package com.shopstack.repository;

import com.shopstack.entity.Payment;
import com.shopstack.entity.PaymentStatus;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    long countByStatusAndPaymentDateBetween(PaymentStatus status,
                                            LocalDateTime start,
                                            LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status " +
           "AND p.paymentDate BETWEEN :start AND :end")
    BigDecimal sumAmountByStatusBetween(@Param("status") PaymentStatus status,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT p.payment_method AS paymentMethod,
                   COUNT(p.id) AS count,
                   COALESCE(SUM(p.amount), 0) AS amount
            FROM payments p
            WHERE p.payment_date BETWEEN :start AND :end
              AND (:status IS NULL OR p.status = :status)
            GROUP BY p.payment_method
            ORDER BY count DESC
            """, nativeQuery = true)
    @QueryHints({@QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<PaymentMethodStatProjection> paymentMethodStats(@Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end,
                                                         @Param("status") String status);
}
