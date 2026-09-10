package com.shopstack.dto.warehouse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDistributionRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotEmpty(message = "At least one warehouse distribution must be specified")
    @Valid
    private List<WarehouseDistributionItem> distributions;

    private String notes;
}
