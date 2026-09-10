package com.shopstack.repository;

import com.shopstack.entity.OrderItemWarehouseAllocation;
import com.shopstack.entity.StockAllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemWarehouseAllocationRepository extends JpaRepository<OrderItemWarehouseAllocation, Long> {

    List<OrderItemWarehouseAllocation> findByOrderItemId(Long orderItemId);

    List<OrderItemWarehouseAllocation> findByWarehouseId(Long warehouseId);

    List<OrderItemWarehouseAllocation> findByAllocationStatus(StockAllocationStatus status);

    List<OrderItemWarehouseAllocation> findByWarehouseIdAndAllocationStatus(Long warehouseId, StockAllocationStatus status);

    @Query("SELECT a FROM OrderItemWarehouseAllocation a WHERE a.orderItem.order.id = :orderId")
    List<OrderItemWarehouseAllocation> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT a FROM OrderItemWarehouseAllocation a " +
           "JOIN FETCH a.orderItem oi " +
           "JOIN FETCH oi.order o " +
           "JOIN FETCH oi.product p " +
           "JOIN FETCH a.warehouse w " +
           "ORDER BY a.allocatedAt DESC")
    List<OrderItemWarehouseAllocation> findAllWithDetails();

    @Query("SELECT a FROM OrderItemWarehouseAllocation a " +
           "JOIN FETCH a.orderItem oi " +
           "JOIN FETCH oi.order o " +
           "JOIN FETCH oi.product p " +
           "JOIN FETCH a.warehouse w " +
           "WHERE a.warehouse.id = :warehouseId " +
           "ORDER BY a.allocatedAt DESC")
    List<OrderItemWarehouseAllocation> findByWarehouseIdWithDetails(@Param("warehouseId") Long warehouseId);
}
