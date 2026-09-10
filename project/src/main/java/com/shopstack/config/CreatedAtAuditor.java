package com.shopstack.config;

import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;

/**
 * JPA entity listener that stamps {@code createdAt} on first persistence so test
 * fixtures and seed data don't need to set it manually.
 */
public class CreatedAtAuditor {

    @PrePersist
    void onPrePersist(Object entity) {
        try {
            var field = entity.getClass().getDeclaredField("createdAt");
            field.setAccessible(true);
            if (field.get(entity) == null) {
                field.set(entity, LocalDateTime.now());
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // entity has no createdAt field — nothing to do
        }
    }
}
