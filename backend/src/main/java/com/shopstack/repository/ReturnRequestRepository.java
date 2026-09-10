package com.shopstack.repository;

import com.shopstack.entity.Order;
import com.shopstack.entity.ReturnRequest;
import com.shopstack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    Optional<ReturnRequest> findByReturnNumber(String returnNumber);

    List<ReturnRequest> findByUser(User user);

    Optional<ReturnRequest> findByOrder(Order order);
    
    Optional<ReturnRequest> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    List<ReturnRequest> findByReturnWarehouseId(Long warehouseId);

    List<ReturnRequest> findByOriginalWarehouseId(Long warehouseId);

    List<ReturnRequest> findByStatus(com.shopstack.entity.ReturnStatus status);

    List<ReturnRequest> findByQcStatus(String qcStatus);

    List<ReturnRequest> findByReturnWarehouseIdAndStatus(Long warehouseId, com.shopstack.entity.ReturnStatus status);
}
