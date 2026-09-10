package com.shopstack.review.repository;

import com.shopstack.review.entity.Review;
import com.shopstack.review.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, ReviewStatus status, Pageable pageable);

    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    @Query("""
            SELECT r FROM Review r
            WHERE r.product.vendor.id = :vendorId
            ORDER BY r.createdAt DESC
            """)
    Page<Review> findByProductVendorId(@Param("vendorId") Long vendorId, Pageable pageable);

    @Query("""
            SELECT r FROM Review r
            WHERE r.product.id = :productId AND r.product.vendor.id = :vendorId
            ORDER BY r.createdAt DESC
            """)
    Page<Review> findByProductIdAndVendorId(@Param("productId") Long productId,
                                             @Param("vendorId") Long vendorId,
                                             Pageable pageable);

    @Query("""
            SELECT r.rating, COUNT(r)
            FROM Review r
            WHERE r.product.id = :productId AND r.status = com.shopstack.review.enums.ReviewStatus.APPROVED
            GROUP BY r.rating
            """)
    List<Object[]> countByRatingGroupForProduct(@Param("productId") Long productId);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0), COUNT(r)
            FROM Review r
            WHERE r.product.id = :productId AND r.status = com.shopstack.review.enums.ReviewStatus.APPROVED
            """)
    Object[] calculateAverageAndCountForProduct(@Param("productId") Long productId);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0), COUNT(r)
            FROM Review r
            WHERE r.product.vendor.id = :vendorId AND r.status = com.shopstack.review.enums.ReviewStatus.APPROVED
            """)
    Object[] calculateAverageAndCountForVendor(@Param("vendorId") Long vendorId);

    long countByProductIdAndStatus(Long productId, ReviewStatus status);
}
