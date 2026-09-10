package com.shopstack.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesDataPoint(
        LocalDate date,
        long orderCount,
        BigDecimal revenue
) {
}
