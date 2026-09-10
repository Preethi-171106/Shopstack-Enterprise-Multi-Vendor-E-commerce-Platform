package com.shopstack.common.repository;

import com.shopstack.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query(value = """
            SELECT COUNT(DISTINCT u.id) FROM users u
            JOIN user_roles ur ON ur.user_id = u.id
            WHERE ur.role = :role
            """, nativeQuery = true)
    long countByRole(@Param("role") String role);
}
