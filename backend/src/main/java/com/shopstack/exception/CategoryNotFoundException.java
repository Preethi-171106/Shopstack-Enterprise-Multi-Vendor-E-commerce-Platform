package com.shopstack.exception;

/**
 * CategoryNotFoundException — Thrown when a requested category does not exist
 * or is not visible (e.g., inactive category on public endpoints).
 *
 * <p>Mapped to HTTP 404 Not Found by {@link GlobalExceptionHandler}.
 */
public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(String message) {
        super(message);
    }
}
