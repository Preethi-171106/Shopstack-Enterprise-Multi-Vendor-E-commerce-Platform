package com.shopstack.service;

import com.shopstack.dto.VendorProfileCreateRequest;
import com.shopstack.dto.VendorProfileResponse;
import com.shopstack.dto.VendorProfileUpdateRequest;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.entity.VendorStatus;
import com.shopstack.exception.InvalidCredentialsException;
import com.shopstack.exception.VendorProfileAlreadyExistsException;
import com.shopstack.exception.VendorProfileNotFoundException;
import com.shopstack.mapper.VendorProfileMapper;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VendorService — Business service managing vendor store profile workflows.
 */
@Service
public class VendorService {

    private final VendorProfileRepository vendorProfileRepository;
    private final UserRepository userRepository;
    private final VendorProfileMapper vendorProfileMapper;

    public VendorService(
            VendorProfileRepository vendorProfileRepository,
            UserRepository userRepository,
            VendorProfileMapper vendorProfileMapper
    ) {
        this.vendorProfileRepository = vendorProfileRepository;
        this.userRepository = userRepository;
        this.vendorProfileMapper = vendorProfileMapper;
    }

    /**
     * Creates a new store profile for the authenticated vendor user.
     *
     * @param userEmail the authenticated vendor's email
     * @param request the creation request body containing store information
     * @return {@link VendorProfileResponse} with status PENDING
     */
    @Transactional
    public VendorProfileResponse createVendorProfile(String userEmail, VendorProfileCreateRequest request) {
        User user = getUserByEmail(userEmail);
        verifyVendorRole(user);

        if (vendorProfileRepository.existsByUserId(user.getId())) {
            throw new VendorProfileAlreadyExistsException("Vendor profile already exists");
        }

        VendorProfile profile = VendorProfile.builder()
                .user(user)
                .storeName(request.storeName().trim())
                .storeDescription(request.storeDescription() != null ? request.storeDescription().trim() : null)
                .businessEmail(request.businessEmail() != null ? request.businessEmail().trim().toLowerCase() : null)
                .businessPhone(request.businessPhone() != null ? request.businessPhone().trim() : null)
                .businessAddress(request.businessAddress() != null ? request.businessAddress().trim() : null)
                .city(request.city() != null ? request.city().trim() : null)
                .state(request.state() != null ? request.state().trim() : null)
                .country(request.country() != null ? request.country().trim() : null)
                .postalCode(request.postalCode() != null ? request.postalCode().trim() : null)
                .status(VendorStatus.PENDING) // Always PENDING on initial creation
                .build();

        VendorProfile savedProfile = vendorProfileRepository.save(profile);
        return vendorProfileMapper.toResponse(savedProfile);
    }

    /**
     * Retrieves the current authenticated vendor's store profile.
     *
     * @param userEmail the authenticated vendor's email
     * @return {@link VendorProfileResponse}
     */
    @Transactional(readOnly = true)
    public VendorProfileResponse getVendorProfile(String userEmail) {
        User user = getUserByEmail(userEmail);
        verifyVendorRole(user);

        VendorProfile profile = vendorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new VendorProfileNotFoundException("Vendor profile not found"));

        return vendorProfileMapper.toResponse(profile);
    }

    /**
     * Updates the current authenticated vendor's store profile details.
     *
     * @param userEmail the authenticated vendor's email
     * @param request the update request containing editable fields
     * @return {@link VendorProfileResponse}
     */
    @Transactional
    public VendorProfileResponse updateVendorProfile(String userEmail, VendorProfileUpdateRequest request) {
        User user = getUserByEmail(userEmail);
        verifyVendorRole(user);

        VendorProfile profile = vendorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new VendorProfileNotFoundException("Vendor profile not found"));

        // Update editable fields
        profile.setStoreName(request.storeName().trim());
        profile.setStoreDescription(request.storeDescription() != null ? request.storeDescription().trim() : null);
        profile.setBusinessEmail(request.businessEmail() != null ? request.businessEmail().trim().toLowerCase() : null);
        profile.setBusinessPhone(request.businessPhone() != null ? request.businessPhone().trim() : null);
        profile.setBusinessAddress(request.businessAddress() != null ? request.businessAddress().trim() : null);
        profile.setCity(request.city() != null ? request.city().trim() : null);
        profile.setState(request.state() != null ? request.state().trim() : null);
        profile.setCountry(request.country() != null ? request.country().trim() : null);
        profile.setPostalCode(request.postalCode() != null ? request.postalCode().trim() : null);

        VendorProfile updatedProfile = vendorProfileRepository.save(profile);
        return vendorProfileMapper.toResponse(updatedProfile);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(InvalidCredentialsException::new);
    }

    private void verifyVendorRole(User user) {
        if (user.getRole() != UserRole.VENDOR) {
            throw new AccessDeniedException("You do not have permission to access vendor profile features");
        }
    }
}
