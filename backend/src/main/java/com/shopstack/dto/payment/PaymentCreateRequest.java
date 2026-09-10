package com.shopstack.dto.payment;

import com.shopstack.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PaymentCreateRequest — DTO for initializing a payment transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreateRequest {

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String currency;
}
