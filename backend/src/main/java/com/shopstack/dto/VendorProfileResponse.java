package com.shopstack.dto;

import com.shopstack.entity.VendorStatus;

import java.time.LocalDateTime;

/**
 * VendorProfileResponse — Output DTO returning safe vendor profile information.
 */
public record VendorProfileResponse(
        Long id,
        Long userId,
        String storeName,
        String storeDescription,
        String businessEmail,
        String businessPhone,
        String businessAddress,
        String city,
        String state,
        String country,
        String postalCode,
        VendorStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
