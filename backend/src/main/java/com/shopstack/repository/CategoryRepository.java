package com.shopstack.repository;

import com.shopstack.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CategoryRepository — Spring Data JPA repository for {@link Category} entity.
 *
 * <p>Provides CRUD operations and custom queries for category management.
 * Case-insensitive name checks enforce uniqueness constraints at the service layer
 * before hitting the database unique index.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Checks if a category with the given name exists, case-insensitively.
     * Used to prevent duplicate creation.
     *
     * @param name the category name to check
     * @return {@code true} if a category with this name already exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Checks if a category with the given name exists, case-insensitively,
     * excluding a specific category ID. Used during update to allow same-name updates.
     *
     * @param name the category name to check
     * @param id   the ID to exclude (the category being updated)
     * @return {@code true} if another category with this name exists
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Checks if a category with the given slug already exists.
     * Used as a secondary unique guard in case of slug collisions.
     *
     * @param slug the slug to check
     * @return {@code true} if a category with this slug already exists
     */
    boolean existsBySlug(String slug);

    /**
     * Checks if a category with the given slug exists, excluding a specific ID.
     * Used during update to detect slug conflicts with other categories.
     *
     * @param slug the slug to check
     * @param id   the ID to exclude (the category being updated)
     * @return {@code true} if another category with this slug exists
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * Retrieves all currently active categories. Used for the public listing endpoint.
     *
     * @return list of active {@link Category} entities
     */
    List<Category> findByActiveTrue();

    /**
     * Retrieves a category by ID only if it is active. Used for the public detail endpoint.
     * Returns empty if the category is inactive or does not exist.
     *
     * @param id the category ID
     * @return an {@link Optional} containing the active category, or empty
     */
    Optional<Category> findByIdAndActiveTrue(Long id);

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentCategoryId(Long parentCategoryId);

    boolean existsByParentCategoryId(Long parentCategoryId);
}
