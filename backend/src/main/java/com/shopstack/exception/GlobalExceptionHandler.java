package com.shopstack.exception;

import com.shopstack.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — Centralized REST exception handling.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles authentication failures (HTTP 401 Unauthorized).
     */
    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Invalid email or password",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles disabled user login attempts (HTTP 401 Unauthorized).
     */
    @ExceptionHandler(UserDisabledException.class)
    public ResponseEntity<ErrorResponse> handleUserDisabled(
            UserDisabledException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles pending, rejected, or suspended warehouse staff login attempts (HTTP 403 Forbidden).
     */
    @ExceptionHandler(WarehouseStaffAccountStatusException.class)
    public ResponseEntity<ErrorResponse> handleWarehouseStaffStatus(
            WarehouseStaffAccountStatusException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles duplicate email, duplicate vendor profile, duplicate category, and duplicate inventory errors (HTTP 409 Conflict).
     */
    @ExceptionHandler({
            EmailAlreadyExistsException.class,
            VendorProfileAlreadyExistsException.class,
            CategoryAlreadyExistsException.class,
            ProductAlreadyExistsException.class,
            ShipmentAlreadyExistsException.class,
            DuplicateInventoryException.class,
            DuplicatePaymentException.class,
            CouponAlreadyExistsException.class,
            DuplicateReturnException.class,
            WarehouseAlreadyExistsException.class
    })
    public ResponseEntity<ErrorResponse> handleConflictExceptions(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles missing entities (HTTP 404 Not Found).
     */
    @ExceptionHandler({
            VendorProfileNotFoundException.class,
            CategoryNotFoundException.class,
            ProductNotFoundException.class,
            CartItemNotFoundException.class,
            WishlistItemNotFoundException.class,
            ShipmentNotFoundException.class,
            OrderNotFoundException.class,
            InventoryNotFoundException.class,
            PaymentNotFoundException.class,
            CouponNotFoundException.class,
            ReturnNotFoundException.class,
            NotificationNotFoundException.class,
            WarehouseNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundExceptions(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles product ownership violations — vendor accessing another vendor's product (HTTP 403 Forbidden).
     */
    @ExceptionHandler({ProductOwnershipException.class, ShipmentOwnershipException.class})
    public ResponseEntity<ErrorResponse> handleOwnershipExceptions(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles business rule violations such as stock limits or pricing errors (HTTP 400 Bad Request).
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class,
            InsufficientStockException.class,
            InvalidShipmentStatusTransitionException.class,
            InvalidStockOperationException.class,
            PaymentVerificationException.class,
            RefundNotAllowedException.class,
            CouponExpiredException.class,
            CouponInactiveException.class,
            CouponUsageExceededException.class,
            CouponMinimumAmountException.class,
            InvalidCouponException.class,
            InvalidReturnException.class,
            ReturnNotAllowedException.class,
            RefundFailedException.class,
            WarehouseAllocationException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles Bean Validation errors on DTOs (HTTP 400 Bad Request).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input validation failed for one or more fields",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles missing Razorpay configuration (HTTP 503 Service Unavailable).
     */
    @ExceptionHandler(com.shopstack.exception.RazorpayNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleRazorpayNotConfigured(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Fallback handler for uncaught exceptions (HTTP 500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        // Log the full stack trace so root causes are visible in application logs
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .error("Unhandled exception on {} {}: {}", request.getMethod(), request.getRequestURI(),
                        ex.getMessage(), ex);

        // If root cause is more descriptive, surface it
        String message = ex.getMessage();
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause.getMessage() != null && !cause.getMessage().equals(message)) {
                message = cause.getMessage();
            }
            cause = cause.getCause();
        }

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
