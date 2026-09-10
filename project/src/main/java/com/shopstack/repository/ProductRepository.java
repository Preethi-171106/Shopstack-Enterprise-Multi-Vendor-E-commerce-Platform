package com.shopstack.repository;

import com.shopstack.entity.Product;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    long countByVendorId(Long vendorId);

    long countByStockQuantity(Integer quantity);

    long countByStockQuantityLessThan(Integer quantity);

    long countByStockQuantityLessThanEqual(Integer quantity);

    @Query("SELECT COALESCE(SUM(p.price * p.stockQuantity), 0) FROM Product p")
    BigDecimal totalStockValue();

    @Query("SELECT COALESCE(SUM(p.price * p.stockQuantity), 0) FROM Product p WHERE p.vendor.id = :vendorId")
    BigDecimal totalStockValueByVendor(@Param("vendorId") Long vendorId);

    List<Product> findByStockQuantity(Integer quantity);

    List<Product> findByStockQuantityLessThan(Integer quantity);

    List<Product> findByStockQuantityLessThanEqual(Integer quantity);

    @Query("SELECT p FROM Product p WHERE (:vendorId IS NULL OR p.vendor.id = :vendorId) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) ORDER BY p.id")
    @QueryHints({@QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Product> findForInventory(@Param("vendorId") Long vendorId,
                                   @Param("categoryId") Long categoryId);
}
