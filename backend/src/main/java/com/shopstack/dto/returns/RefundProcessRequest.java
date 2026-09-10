package com.shopstack.dto.returns;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundProcessRequest {

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
