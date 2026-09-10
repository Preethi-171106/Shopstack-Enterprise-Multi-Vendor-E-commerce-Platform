package com.shopstack.repository;

import java.math.BigDecimal;

public interface ProductRevenueProjection {
    Long getProductId();
    String getProductName();
    BigDecimal getRevenue();
    Long getQuantitySold();
}
