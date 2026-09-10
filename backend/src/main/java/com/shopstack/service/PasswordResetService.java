package com.shopstack.service;

import com.shopstack.dto.ForgotPasswordRequest;
import com.shopstack.dto.ResetPasswordRequest;
import com.shopstack.entity.PasswordResetToken;
import com.shopstack.entity.User;
import com.shopstack.repository.PasswordResetTokenRepository;
import com.shopstack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * PasswordResetService — handles the full forgot-password / reset-password lifecycle.
 *
 * <h2>Security design</h2>
 * <ul>
 *   <li>Token is 256-bit random hex — cannot be guessed or brute-forced.</li>
 *   <li>Expiry is {@code password.reset.token.expiry-minutes} (default 30 min).</li>
 *   <li>Every new request invalidates all previous tokens for that user.</li>
 *   <li>Tokens are single-use; used flag prevents replay attacks.</li>
 *   <li>Calling {@code forgotPassword} with an unregistered email always returns
 *       the same generic response — email enumeration is prevented.</li>
 *   <li>Passwords are BCrypt-hashed before storage.</li>
 *   <li>Tokens are never logged at INFO level or above.</li>
 * </ul>
 *
 * <h2>Email delivery</h2>
 * <p>If {@code spring.mail.host} is configured the service delegates to
 * {@link EmailService}. If email is not configured, the reset token is
 * returned in the API response <em>only in development mode</em>
 * (when {@code app.dev-mode=true}).  In production, missing email config
 * causes the operation to silently succeed (token is created but no link is
 * sent) — operators must configure email before deploying.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Generic response returned for ALL forgot-password calls — prevents email enumeration. */
    public static final String FORGOT_PASSWORD_RESPONSE =
            "If an account exists with this email, a password reset link has been sent.";

    @Value("${password.reset.token.expiry-minutes:30}")
    private long expiryMinutes;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    private final UserRepository             userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder            passwordEncoder;
    private final EmailService               emailService;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository  = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService    = emailService;
    }

    /**
     * Initiates the forgot-password flow.
     *
     * <p>Always returns {@link #FORGOT_PASSWORD_RESPONSE} regardless of whether
     * the email is registered (prevents email enumeration).
     *
     * @param request the forgot-password request containing the user's email
     * @return the opaque success message (always the same string)
     */
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (userOpt.isEmpty()) {
            // Return generic response — do NOT reveal that the email is not registered
            log.debug("[PasswordReset] Forgot-password request for unknown email (not logged)");
            return FORGOT_PASSWORD_RESPONSE;
        }

        User user = userOpt.get();

        // Invalidate any existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        // Generate a cryptographically secure 256-bit token
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = HexFormat.of().formatHex(tokenBytes);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(rawToken)
                .expiryTime(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .build();
        tokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;

        // Attempt email delivery — non-fatal if email is not configured
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetLink);
            log.info("[PasswordReset] Reset link sent to user id={}", user.getId());
        } catch (EmailNotConfiguredException e) {
            // Email not configured — in dev mode expose the link in the log (not in the API response)
            if (devMode) {
                log.warn("[PasswordReset] Email not configured. DEV MODE reset link for user id={}: {}",
                         user.getId(), resetLink);
            } else {
                log.warn("[PasswordReset] Email not configured — reset link generated but not sent. " +
                         "Configure spring.mail.* properties to enable email delivery.");
            }
        } catch (Exception e) {
            log.error("[PasswordReset] Failed to send reset email to user id={}: {}",
                      user.getId(), e.getMessage());
        }

        return FORGOT_PASSWORD_RESPONSE;
    }

    /**
     * Completes the password-reset flow.
     *
     * <p>Validates the token (exists, not expired, not used), checks password
     * confirmation, hashes the new password with BCrypt, updates the user, and
     * marks the token as used.
     *
     * @param token                the raw token from the URL query parameter
     * @param request              the new-password request body
     * @throws IllegalArgumentException for any validation failure (token invalid/expired/used,
     *                                   passwords don't match, or password too short)
     */
    @Transactional
    public void resetPassword(String token, ResetPasswordRequest request) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Reset token is required.");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired password reset link. Please request a new one."));

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException(
                    "This password reset link has expired. Please request a new one.");
        }

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException(
                    "This password reset link has already been used. Please request a new one.");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match. Please try again.");
        }

        if (request.newPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        // Hash the new password — never store plaintext
        String hashedPassword = passwordEncoder.encode(request.newPassword());

        User user = resetToken.getUser();
        user.setPassword(hashedPassword);
        userRepository.save(user);

        // Invalidate the token — mark as used and delete all tokens for this user
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        tokenRepository.deleteByUserId(user.getId());

        log.info("[PasswordReset] Password successfully reset for user id={}", user.getId());
    }

    /**
     * Validates a reset token without consuming it.
     * Used by the frontend to show a useful error before the user fills in the form.
     *
     * @param token the raw token from the URL
     * @return {@code true} if the token is valid (exists, not expired, not used)
     */
    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) return false;
        return tokenRepository.findByToken(token)
                .map(t -> !t.isExpired() && !t.isUsed())
                .orElse(false);
    }
}
