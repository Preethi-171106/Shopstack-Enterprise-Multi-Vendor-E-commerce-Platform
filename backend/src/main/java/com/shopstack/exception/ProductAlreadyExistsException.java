package com.shopstack.exception;

/**
 * ProductAlreadyExistsException — thrown when a product creation or update
 * would violate a unique constraint on SKU or slug.
 *
 * <p>Mapped to HTTP 409 Conflict in {@link GlobalExceptionHandler}.
 */
public class ProductAlreadyExistsException extends RuntimeException {

    public ProductAlreadyExistsException(String message) {
        super(message);
    }
}
