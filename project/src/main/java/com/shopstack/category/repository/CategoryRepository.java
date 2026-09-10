package com.shopstack.category.repository;

import com.shopstack.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    long countByActiveTrue();
}
