package com.shopstack.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponse {
    private Long id;
    private String warehouseCode;
    private String name;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Boolean active;
    private Integer totalSkus;
    private Long totalStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
