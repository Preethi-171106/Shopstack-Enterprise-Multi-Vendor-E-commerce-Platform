package com.shopstack.mapper;

import com.shopstack.dto.warehouse.AllocationResponse;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderItem;
import com.shopstack.entity.OrderItemWarehouseAllocation;
import com.shopstack.entity.Product;
import com.shopstack.entity.User;
import com.shopstack.entity.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class AllocationMapper {

    public AllocationResponse toResponse(OrderItemWarehouseAllocation alloc) {
        if (alloc == null) return null;

        OrderItem oi = alloc.getOrderItem();
        Order order = oi != null ? oi.getOrder() : null;
        Product product = oi != null ? oi.getProduct() : null;
        User user = order != null ? order.getUser() : null;
        Warehouse w = alloc.getWarehouse();

        String customerName = "Customer";
        if (user != null) {
            String first = user.getFirstName() != null ? user.getFirstName() : "";
            String last = user.getLastName() != null ? user.getLastName() : "";
            customerName = (first + " " + last).trim();
            if (customerName.isEmpty()) customerName = user.getEmail();
        }

        String prodName = oi != null && oi.getProductNameSnapshot() != null
                ? oi.getProductNameSnapshot()
                : (product != null ? product.getName() : "Product");
        String prodSku = product != null ? product.getSku() : "N/A";
        String prodImg = oi != null && oi.getProductImageUrlSnapshot() != null
                ? oi.getProductImageUrlSnapshot()
                : (product != null ? product.getImageUrl() : null);

        return AllocationResponse.builder()
                .id(alloc.getId())
                .orderItemId(oi != null ? oi.getId() : null)
                .orderId(order != null ? order.getId() : null)
                .orderNumber(order != null ? order.getOrderNumber() : null)
                .customerId(user != null ? user.getId() : null)
                .customerName(customerName)
                .customerEmail(user != null ? user.getEmail() : null)
                .shippingAddress(order != null ? order.getShippingAddress() : null)
                .productId(product != null ? product.getId() : null)
                .productName(prodName)
                .productSku(prodSku)
                .productImageUrl(prodImg)
                .unitPrice(oi != null ? oi.getPrice() : null)
                .warehouseId(w != null ? w.getId() : null)
                .warehouseCode(w != null ? w.getWarehouseCode() : null)
                .warehouseName(w != null ? w.getName() : null)
                .warehouseCity(w != null ? w.getCity() : null)
                .allocatedQuantity(alloc.getAllocatedQuantity())
                .allocationStatus(alloc.getAllocationStatus() != null ? alloc.getAllocationStatus().name() : "ALLOCATED")
                .allocatedAt(alloc.getAllocatedAt())
                .pickedAt(alloc.getPickedAt())
                .packedAt(alloc.getPackedAt())
                .readyForShipmentAt(alloc.getReadyForShipmentAt())
                .notes(alloc.getNotes())
                .build();
    }
}
