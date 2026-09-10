/**
 * Repository layer for ShopStack.
 *
 * <p>This package will contain Spring Data JPA repository interfaces.
 * A repository is responsible for all database access — reading and writing data.
 *
 * <p><b>What is a Repository?</b><br>
 * Instead of writing raw SQL, we create interfaces that extend Spring's
 * {@code JpaRepository}. Spring automatically generates the implementation
 * at runtime. For example:
 * <pre>
 * public interface ProductRepository extends JpaRepository&lt;Product, Long&gt; {
 *     List&lt;Product&gt; findByCategory(String category);
 * }
 * </pre>
 *
 * <p>Repositories will be added in future milestones when entities are designed.
 */
package com.shopstack.repository;
