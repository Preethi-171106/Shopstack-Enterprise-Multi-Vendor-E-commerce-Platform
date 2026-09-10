package com.shopstack.repository;

import java.math.BigDecimal;

public interface CustomerSummaryProjection {
    Long getCustomerId();
    String getCustomerName();
    String getEmail();
    BigDecimal getTotalSpent();
    Long getOrdersCount();
}
