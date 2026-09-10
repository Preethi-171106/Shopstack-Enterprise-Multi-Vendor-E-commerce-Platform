package com.shopstack.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * EmailService — sends transactional emails (e.g. password reset).
 *
 * <p>Requires {@code spring.mail.host} (and related properties) to be configured.
 * If {@link JavaMailSender} is not available (bean not present), throws
 * {@link EmailNotConfiguredException} — callers must handle this gracefully.
 *
 * <p>Configure email via environment variables:
 * <pre>
 *   MAIL_HOST=smtp.gmail.com
 *   MAIL_PORT=587
 *   MAIL_USERNAME=your-email@gmail.com
 *   MAIL_PASSWORD=your-app-password
 * </pre>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.from:noreply@shopstack.com}")
    private String fromAddress;

    @Value("${app.name:ShopStack}")
    private String appName;

    /** Null when email is not configured — checked before every send. */
    private final JavaMailSender mailSender;

    /**
     * Spring injects {@code null} here when no {@link JavaMailSender} bean exists.
     * This happens when {@code spring.mail.host} is not set — email is optional.
     */
    public EmailService(
            @org.springframework.lang.Nullable JavaMailSender mailSender
    ) {
        this.mailSender = mailSender;
    }

    /**
     * Sends the password-reset email.
     *
     * @param toEmail       recipient email address
     * @param firstName     recipient's first name (for personalisation)
     * @param resetLink     the full reset URL with token
     * @throws EmailNotConfiguredException if SMTP is not configured
     */
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetLink) {
        if (mailSender == null) {
            throw new EmailNotConfiguredException(
                    "Email is not configured. Set MAIL_HOST, MAIL_PORT, MAIL_USERNAME and MAIL_PASSWORD.");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(appName + " — Password Reset Request");
            message.setText(buildPasswordResetBody(firstName, resetLink));
            mailSender.send(message);
            log.debug("[EmailService] Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("[EmailService] Failed to send password reset email: {}", e.getMessage());
            throw e;
        }
    }

    private String buildPasswordResetBody(String firstName, String resetLink) {
        return String.format("""
                Hello %s,
                
                You requested a password reset for your %s account.
                
                Click the link below to reset your password (expires in 30 minutes):
                
                %s
                
                If you did not request a password reset, please ignore this email.
                Your password will remain unchanged.
                
                For security, this link can only be used once.
                
                — The %s Team
                """,
                firstName != null ? firstName : "there",
                appName,
                resetLink,
                appName);
    }
}
