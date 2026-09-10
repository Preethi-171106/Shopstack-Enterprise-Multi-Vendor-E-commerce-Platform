package com.shopstack.dto.returns;

import com.shopstack.entity.ReturnReason;
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
public class ReturnRequestCreateRequest {

    @NotNull(message = "Reason for return is required")
    private ReturnReason reason;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Long orderItemId;

    private Integer quantity;
}
