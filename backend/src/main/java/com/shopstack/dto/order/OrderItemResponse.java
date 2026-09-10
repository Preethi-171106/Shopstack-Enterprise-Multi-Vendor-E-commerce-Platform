package com.shopstack.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private Long vendorProfileId;
    private String vendorStoreName;
    private int quantity;
    private BigDecimal price;
    private BigDecimal unitPriceSnapshot;
    private BigDecimal lineTotal;

}
