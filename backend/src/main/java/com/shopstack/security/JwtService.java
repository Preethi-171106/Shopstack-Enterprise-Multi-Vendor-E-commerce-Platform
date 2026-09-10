package com.shopstack.security;

import com.shopstack.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtService — Responsible for generating, parsing, and validating JSON Web Tokens (JWT).
 *
 * <p>Uses JJWT 0.12.x for HMAC-SHA256 signing.
 */
@Service
public class JwtService {

    private final String secretKey;
    private final long jwtExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secretKey,
            @Value("${app.jwt.expiration:86400000}") long jwtExpirationMs
    ) {
        // Validate secret key length (must be at least 256 bits after Base64 decoding)
        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(secretKey);
        } catch (Exception e) {
            decoded = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        if (decoded.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes) long after Base64 decoding.");
        }
        this.secretKey = secretKey;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Returns the JWT expiration duration in milliseconds.
     */
    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Generates a signed JWT token for the given user.
     *
     * @param user the persistent user entity
     * @return compact JWT token string
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        return generateToken(extraClaims, user.getEmail());
    }

    /**
     * Generates a signed JWT token with extra claims and a subject (email).
     */
    public String generateToken(Map<String, Object> extraClaims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the subject (email) from a JWT token.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Validates a token against a user's email.
     */
    public boolean isTokenValid(String token, String userEmail) {
        try {
            final String email = extractEmail(token);
            return (email.equalsIgnoreCase(userEmail) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks if a token has expired.
     */
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Extracts all claims from a JWT token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Constructs the SecretKey for HMAC signing.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (Exception e) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
