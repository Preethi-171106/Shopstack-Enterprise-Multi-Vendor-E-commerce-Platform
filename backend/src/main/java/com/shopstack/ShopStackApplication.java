package com.shopstack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ShopStackApplication — Main entry point for the ShopStack backend.
 *
 * <p>The {@code @SpringBootApplication} annotation is a convenience annotation that combines:
 * <ul>
 *   <li>{@code @Configuration}     — Marks this class as a source of bean definitions</li>
 *   <li>{@code @EnableAutoConfiguration} — Tells Spring Boot to auto-configure based on
 *       the dependencies on the classpath (e.g., PostgreSQL driver → auto-configure JPA)</li>
 *   <li>{@code @ComponentScan}     — Scans all sub-packages of {@code com.shopstack}
 *       for Spring components (controllers, services, repositories, etc.)</li>
 * </ul>
 *
 * <p><b>Architecture Overview:</b>
 * <pre>
 * HTTP Request
 *      ↓
 * Controller Layer   (com.shopstack.controller)  — Receives HTTP, validates input
 *      ↓
 * Service Layer      (com.shopstack.service)     — Business logic lives here
 *      ↓
 * Repository Layer   (com.shopstack.repository)  — Database queries (Spring Data JPA)
 *      ↓
 * Database           (PostgreSQL)                — Persistent data storage
 * </pre>
 */
@SpringBootApplication
public class ShopStackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopStackApplication.class, args);
    }

}
