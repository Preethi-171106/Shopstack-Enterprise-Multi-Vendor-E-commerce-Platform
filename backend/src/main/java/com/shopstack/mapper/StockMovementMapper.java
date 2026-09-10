package com.shopstack.mapper;

import com.shopstack.dto.inventory.StockMovementResponse;
import com.shopstack.entity.StockMovement;
import org.springframework.stereotype.Component;

/**
 * StockMovementMapper — Maps {@link StockMovement} entities to {@link StockMovementResponse} DTOs.
 */
@Component
public class StockMovementMapper {

    public StockMovementResponse toResponse(StockMovement movement) {
        if (movement == null) {
            return null;
        }

        Long productId = movement.getProduct() != null ? movement.getProduct().getId() : null;
        String productName = movement.getProduct() != null ? movement.getProduct().getName() : null;
        String productSku = movement.getProduct() != null ? movement.getProduct().getSku() : null;

        Long userId = movement.getPerformedBy() != null ? movement.getPerformedBy().getId() : null;
        String userEmail = movement.getPerformedBy() != null ? movement.getPerformedBy().getEmail() : null;

        return new StockMovementResponse(
                movement.getId(),
                productId,
                productName,
                productSku,
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getPreviousStock(),
                movement.getNewStock(),
                movement.getReferenceId(),
                movement.getNotes(),
                userId,
                userEmail,
                movement.getCreatedAt()
        );
    }
}
