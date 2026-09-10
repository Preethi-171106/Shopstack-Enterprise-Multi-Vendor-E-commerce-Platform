package com.shopstack.dto.returns;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnReceiveRequest {

    @Size(max = 100, message = "Package condition description must not exceed 100 characters")
    private String packageCondition;

    @Size(max = 1000, message = "Receiving notes must not exceed 1000 characters")
    private String receivingNotes;

    @Min(value = 1, message = "Received quantity must be at least 1")
    private Integer receivedQuantity;
}
