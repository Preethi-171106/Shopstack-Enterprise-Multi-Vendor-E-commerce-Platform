package com.shopstack.repository;

import java.math.BigDecimal;

public interface InventoryItemProjection {
    Long getProductId();
    String getProductName();
    Integer getStockQuantity();
    Integer getLowStockThreshold();
    BigDecimal getUnitPrice();
}
