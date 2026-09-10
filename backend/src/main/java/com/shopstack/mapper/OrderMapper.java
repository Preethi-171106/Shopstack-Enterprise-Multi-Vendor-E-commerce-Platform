package com.shopstack.mapper;

import com.shopstack.dto.order.OrderItemResponse;
import com.shopstack.dto.order.OrderResponse;
import com.shopstack.entity.Order;
import com.shopstack.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .orderStatus(order.getOrderStatus())
                .subtotalAmount(order.getSubtotalAmount() != null ? order.getSubtotalAmount() : order.getTotalAmount())
                .discountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO)
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
                .shippingAddress(order.getShippingAddress())
                .items(order.getItems() != null ?
                        order.getItems().stream().map(this::toOrderItemResponse).collect(Collectors.toList()) :
                        List.of())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }

        // Prefer snapshot fields (captured at order time) — fall back to live product data
        // for orders that existed before snapshots were added.
        String productName = item.getProductNameSnapshot() != null
                ? item.getProductNameSnapshot()
                : (item.getProduct() != null ? item.getProduct().getName() : null);

        String productImageUrl = item.getProductImageUrlSnapshot() != null
                ? item.getProductImageUrlSnapshot()
                : (item.getProduct() != null ? item.getProduct().getImageUrl() : null);

        BigDecimal unitPrice = item.getUnitPriceSnapshot() != null
                ? item.getUnitPriceSnapshot()
                : item.getPrice();

        BigDecimal lineTotal = unitPrice != null
                ? unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                : null;

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(productName)
                .productSku(item.getProduct() != null ? item.getProduct().getSku() : null)
                .productImageUrl(productImageUrl)
                .vendorProfileId(item.getVendorProfile() != null ? item.getVendorProfile().getId() : null)
                .vendorStoreName(item.getVendorProfile() != null ? item.getVendorProfile().getStoreName() : null)
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .unitPriceSnapshot(unitPrice)
                .lineTotal(lineTotal)
                .build();
    }
}
