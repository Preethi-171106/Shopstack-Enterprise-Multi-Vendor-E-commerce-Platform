package com.shopstack.repository;

import java.math.BigDecimal;

public interface StatusCountProjection {
    String getStatus();
    Long getCount();
}
