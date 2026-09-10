/**
 * Mapper layer for ShopStack.
 *
 * <p>This package will contain mapper classes that convert between
 * Entity objects and DTO (Data Transfer Object) objects.
 *
 * <p><b>Why separate Entities from DTOs?</b><br>
 * Entities contain database-specific details (table names, column mappings, etc.)
 * that should not be exposed directly to API clients. DTOs contain only the
 * data that is safe and relevant to expose via the API.
 *
 * <p>Mappers translate between these two representations. For example:
 * <pre>
 * Product (entity) → ProductResponse (DTO)  — for GET requests
 * ProductRequest (DTO) → Product (entity)   — for POST/PUT requests
 * </pre>
 *
 * <p>Mappers will be added in future milestones alongside their entities and DTOs.
 */
package com.shopstack.mapper;
