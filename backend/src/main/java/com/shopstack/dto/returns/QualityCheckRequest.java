package com.shopstack.dto.returns;

import com.shopstack.entity.QualityCheckResult;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityCheckRequest {

    @NotNull(message = "Quality check result is required")
    private QualityCheckResult result;

    @Size(max = 1000, message = "Condition notes must not exceed 1000 characters")
    private String conditionNotes;

    @Min(value = 0, message = "Accepted quantity cannot be negative")
    private Integer acceptedQuantity;

    @Min(value = 0, message = "Damaged quantity cannot be negative")
    private Integer damagedQuantity;

    private Boolean moveToQuarantine;
}
