package com.shopstack.repository;

import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository — Spring Data JPA repository for {@link User} entity.
 *
 * <p>Spring Data JPA automatically generates SQL implementations for interface methods at runtime.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Checks if a user already exists with the given email address (case-insensitive).
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Finds a user by email address (case-insensitive).
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Finds all users with a specific role. Used by admin management APIs.
     */
    List<User> findByRole(UserRole role);
}
