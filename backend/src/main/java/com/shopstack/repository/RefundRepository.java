package com.shopstack.repository;

import com.shopstack.entity.Refund;
import com.shopstack.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRefundId(String refundId);

    Optional<Refund> findByReturnRequest(ReturnRequest returnRequest);

    java.util.List<Refund> findByPayment(com.shopstack.entity.Payment payment);
}
