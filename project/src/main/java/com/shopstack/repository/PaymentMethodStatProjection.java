package com.shopstack.repository;

import java.math.BigDecimal;

public interface PaymentMethodStatProjection {
    String getPaymentMethod();
    Long getCount();
    BigDecimal getAmount();
}
