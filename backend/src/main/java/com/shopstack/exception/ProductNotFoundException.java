package com.shopstack.exception;

/**
 * ProductNotFoundException — thrown when a requested product does not exist
 * or is not accessible by the requester (e.g., an inactive product requested publicly).
 *
 * <p>Mapped to HTTP 404 Not Found in {@link GlobalExceptionHandler}.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
