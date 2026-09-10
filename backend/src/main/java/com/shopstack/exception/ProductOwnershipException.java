package com.shopstack.exception;

/**
 * ProductOwnershipException — thrown when an authenticated vendor attempts to
 * access, update, or delete a product that belongs to a different vendor.
 *
 * <p>This enforces the ownership security rule:
 * "A vendor may only manage products that belong to their own vendor profile."
 *
 * <p>Mapped to HTTP 403 Forbidden in {@link GlobalExceptionHandler}.
 *
 * <p>Security note: We return 403 (not 404) here to explicitly communicate
 * that the resource exists but access is denied — consistent with Spring Security's
 * access denied handling for other role-protected endpoints.
 */
public class ProductOwnershipException extends RuntimeException {

    public ProductOwnershipException(String message) {
        super(message);
    }
}
