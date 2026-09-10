package com.shopstack.dto.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * StockAdjustmentRequest — Request DTO to adjust inventory stock.
 * Supports direct newTotalStock setting or delta adjustment operations (ADD, REMOVE, DAMAGE, CORRECTION, RETURN, SET).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record StockAdjustmentRequest(
        @JsonProperty("newTotalStock")
        Integer newTotalStock,

        @JsonProperty("adjustmentType")
        String adjustmentType,

        @JsonProperty("quantity")
        Integer quantity,

        @JsonProperty("reason")
        String reason,

        @JsonProperty("referenceId")
        String referenceId,

        @JsonProperty("notes")
        String notes
) {
    public StockAdjustmentRequest(Integer newTotalStock, String referenceId, String notes) {
        this(newTotalStock, null, null, notes, referenceId, notes);
    }
}

