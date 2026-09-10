package com.shopstack.exception;

/**
 * CategoryAlreadyExistsException — Thrown when attempting to create or update a category
 * with a name that already exists (case-insensitive uniqueness check).
 *
 * <p>Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class CategoryAlreadyExistsException extends RuntimeException {

    public CategoryAlreadyExistsException(String message) {
        super(message);
    }
}
