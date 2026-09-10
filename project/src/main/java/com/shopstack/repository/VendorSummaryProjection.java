package com.shopstack.repository;

import java.math.BigDecimal;

public interface VendorSummaryProjection {
    Long getVendorId();
    String getVendorName();
    BigDecimal getRevenue();
    Long getOrdersCount();
    Long getProductsCount();
}
