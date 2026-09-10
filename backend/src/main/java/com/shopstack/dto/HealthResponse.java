package com.shopstack.dto;

/**
 * HealthResponse — Data Transfer Object (DTO) for the health check API response.
 *
 * <p><b>What is a DTO?</b><br>
 * A DTO (Data Transfer Object) is a simple Java object whose only job is to carry
 * data between layers — from the Service layer to the Controller layer, and then
 * serialized (converted) to JSON before being sent back to the client.
 *
 * <p>DTOs are kept separate from database Entities. The database entity is the
 * "database shape" of data; the DTO is the "API shape" of data. This gives us
 * flexibility to change one without affecting the other.
 *
 * <p><b>JSON Output:</b>
 * <pre>
 * {
 *   "status":  "UP",
 *   "message": "ShopStack backend is running"
 * }
 * </pre>
 *
 * <p>We use a Java {@code record} here (Java 16+) which automatically generates:
 * <ul>
 *   <li>A constructor with all fields</li>
 *   <li>Getter methods (status(), message())</li>
 *   <li>equals(), hashCode(), toString()</li>
 * </ul>
 * Jackson (Spring's JSON library) knows how to serialize records to JSON automatically.
 */
public record HealthResponse(
        String status,
        String message
) {
    // No additional code needed.
    // Java records are immutable and auto-generate all standard methods.
}
