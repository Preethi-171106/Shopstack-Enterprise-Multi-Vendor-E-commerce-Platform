/**
 * Entity layer for ShopStack.
 *
 * <p>This package will contain JPA entity classes that map to database tables.
 * Each entity class represents one table in the PostgreSQL database.
 *
 * <p><b>What is an Entity?</b><br>
 * An entity is a Java class annotated with {@code @Entity} that Hibernate
 * maps to a database table. For example:
 * <pre>
 * {@code @Entity}
 * {@code @Table(name = "products")}
 * public class Product {
 *     {@code @Id}
 *     {@code @GeneratedValue}
 *     private Long id;
 *     private String name;
 *     // ...
 * }
 * </pre>
 *
 * <p>Entities will be designed and added in future milestones (Milestone 2+).
 */
package com.shopstack.entity;
