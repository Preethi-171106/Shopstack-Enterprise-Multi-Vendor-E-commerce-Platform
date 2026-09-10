package com.shopstack.vendor.repository;

import com.shopstack.common.entity.User;
import com.shopstack.vendor.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByUser(User user);

    Optional<Vendor> findByUserId(Long userId);

    long countByActiveTrue();
}
