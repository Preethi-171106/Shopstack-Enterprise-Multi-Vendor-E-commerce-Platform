package com.shopstack.product.repository;

import com.shopstack.product.entity.Product;
import com.shopstack.product.enums.ProductStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    long countByStatus(ProductStatus status);

    long countByVendorId(Long vendorId);

    long countByVendorIdAndStatus(Long vendorId, ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity = 0")
    List<Product> findOutOfStockProducts();

    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0 AND p.stockQuantity <= p.lowStockThreshold")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.vendor.id = :vendorId AND p.stockQuantity = 0")
    List<Product> findOutOfStockProductsByVendorId(@Param("vendorId") Long vendorId);

    @Query("SELECT p FROM Product p WHERE p.vendor.id = :vendorId AND p.stockQuantity > 0 AND p.stockQuantity <= p.lowStockThreshold")
    List<Product> findLowStockProductsByVendorId(@Param("vendorId") Long vendorId);

    @Query("""
            SELECT oi.product.id, oi.product.name, SUM(oi.quantity), SUM(oi.subtotal)
            FROM OrderItem oi
            WHERE oi.order.status = com.shopstack.order.enums.OrderStatus.DELIVERED
            GROUP BY oi.product.id, oi.product.name
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findTopSellingProducts(Pageable pageable);

    @Query("""
            SELECT oi.product.id, oi.product.name, SUM(oi.quantity), SUM(oi.subtotal)
            FROM OrderItem oi
            WHERE oi.order.vendor.id = :vendorId AND oi.order.status = com.shopstack.order.enums.OrderStatus.DELIVERED
            GROUP BY oi.product.id, oi.product.name
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findTopSellingProductsByVendor(@Param("vendorId") Long vendorId, Pageable pageable);
}
