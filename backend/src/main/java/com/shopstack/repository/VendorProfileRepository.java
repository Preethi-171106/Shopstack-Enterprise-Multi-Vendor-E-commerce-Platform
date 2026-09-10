package com.shopstack.repository;

import com.shopstack.entity.VendorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * VendorProfileRepository — Spring Data JPA repository for {@link VendorProfile} entity.
 */
@Repository
public interface VendorProfileRepository extends JpaRepository<VendorProfile, Long> {

    /**
     * Finds a vendor profile by the associated user's primary key ID.
     *
     * @param userId the user's ID
     * @return an {@link Optional} containing the vendor profile if present
     */
    Optional<VendorProfile> findByUserId(Long userId);

    /**
     * Checks if a vendor profile already exists for the given user ID.
     *
     * @param userId the user's ID
     * @return true if a vendor profile exists for the user, false otherwise
     */
    boolean existsByUserId(Long userId);

    /**
     * Finds a vendor profile by the associated user's email address (case-insensitive).
     *
     * <p>Used by {@code ProductService} and {@code InventoryService} to resolve the
     * authenticated vendor's profile from the JWT-extracted email — the primary identity
     * resolution method for vendor-scoped operations.
     *
     * <p>Case-insensitive to guard against any email casing inconsistency between
     * what was stored at registration time and what the JWT subject contains.
     *
     * @param email the user's email address (matched case-insensitively)
     * @return an {@link java.util.Optional} containing the vendor profile if present
     */
    java.util.Optional<VendorProfile> findByUserEmailIgnoreCase(String email);

    /**
     * Finds a vendor profile by the associated user's email address (case-sensitive).
     * Kept for backward compatibility — prefer {@link #findByUserEmailIgnoreCase(String)}.
     *
     * @param email the user's email address
     * @return an {@link java.util.Optional} containing the vendor profile if present
     */
    java.util.Optional<VendorProfile> findByUserEmail(String email);
}
