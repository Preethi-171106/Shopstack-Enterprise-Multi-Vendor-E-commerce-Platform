package com.shopstack.repository;

import java.math.BigDecimal;

public interface RevenueBreakdownProjection {
    String getPeriodLabel();
    BigDecimal getRevenue();
    Long getOrdersCount();
}
