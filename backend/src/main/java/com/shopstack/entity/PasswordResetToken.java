package com.shopstack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * PasswordResetToken — stores a single-use, expiring password-reset token for a user.
 *
 * <p>Security properties:
 * <ul>
 *   <li>Token is a cryptographically random 256-bit hex string — cannot be guessed.</li>
 *   <li>Expires after a configurable window (default 30 minutes).</li>
 *   <li>Single-use: {@code used} flag is set to {@code true} after first successful reset.</li>
 *   <li>Expired or used tokens are rejected by the service layer.</li>
 *   <li>One pending (unused, unexpired) token per user — old tokens are invalidated on new request.</li>
 * </ul>
 */
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_prt_token",   columnList = "token",   unique = true),
                @Index(name = "idx_prt_user_id", columnList = "user_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning user — many tokens may exist historically for the same user. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The actual token value sent in the reset URL.
     * 64 hex characters = 256 bits of entropy (SecureRandom).
     * Stored as plain text because it is a one-time opaque credential, not a password.
     */
    @Column(nullable = false, unique = true, length = 128)
    private String token;

    /**
     * When this token expires. Default: 30 minutes from creation.
     */
    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    /**
     * Whether this token has already been used to reset a password.
     * Once {@code true} the token is permanently rejected.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Convenience: returns {@code true} if the token's expiry time is in the past. */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryTime);
    }

    public LocalDateTime getExpiresAt() {
        return this.expiryTime;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiryTime = expiresAt;
    }
}
