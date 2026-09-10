package com.shopstack.payment.repository;

import com.shopstack.payment.entity.Payment;
import com.shopstack.payment.enums.PaymentMethod;
import com.shopstack.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    long countByStatus(PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = com.shopstack.payment.enums.PaymentStatus.SUCCESS")
    BigDecimal sumSuccessfulPayments();

    @Query("""
            SELECT p.method, COUNT(p), COALESCE(SUM(p.amount), 0)
            FROM Payment p
            GROUP BY p.method
            """)
    List<Object[]> countGroupByMethod();

    @Query("""
            SELECT p.status, COUNT(p), COALESCE(SUM(p.amount), 0)
            FROM Payment p
            GROUP BY p.status
            """)
    List<Object[]> countGroupByStatus();

    @Query("""
            SELECT p.method, COUNT(p), COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = com.shopstack.payment.enums.PaymentStatus.SUCCESS
            GROUP BY p.method
            """)
    List<Object[]> countSuccessfulGroupByMethod();

    long countByMethod(PaymentMethod method);
}
