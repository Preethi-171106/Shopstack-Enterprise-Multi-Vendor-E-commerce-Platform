package com.shopstack.mapper;

import com.shopstack.dto.VendorProfileResponse;
import com.shopstack.entity.VendorProfile;
import org.springframework.stereotype.Component;

/**
 * VendorProfileMapper — Component mapping VendorProfile entities to VendorProfileResponse DTOs.
 */
@Component
public class VendorProfileMapper {

    /**
     * Converts a {@link VendorProfile} entity to a {@link VendorProfileResponse} DTO.
     *
     * @param profile the persistent vendor profile entity
     * @return {@link VendorProfileResponse} containing client-safe fields
     */
    public VendorProfileResponse toResponse(VendorProfile profile) {
        if (profile == null) {
            return null;
        }
        return new VendorProfileResponse(
                profile.getId(),
                profile.getUser() != null ? profile.getUser().getId() : null,
                profile.getStoreName(),
                profile.getStoreDescription(),
                profile.getBusinessEmail(),
                profile.getBusinessPhone(),
                profile.getBusinessAddress(),
                profile.getCity(),
                profile.getState(),
                profile.getCountry(),
                profile.getPostalCode(),
                profile.getStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
