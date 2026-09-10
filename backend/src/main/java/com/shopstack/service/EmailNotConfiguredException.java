package com.shopstack.service;

/**
 * EmailNotConfiguredException — thrown when EmailService is called but SMTP
 * is not configured in the application properties.
 *
 * <p>Callers should handle this gracefully (e.g. fall back to dev-mode logging).
 */
public class EmailNotConfiguredException extends RuntimeException {
    public EmailNotConfiguredException(String message) {
        super(message);
    }
}
