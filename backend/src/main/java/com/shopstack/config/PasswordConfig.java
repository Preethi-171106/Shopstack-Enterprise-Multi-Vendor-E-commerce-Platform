package com.shopstack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordConfig — Configuration for password hashing.
 *
 * <p><b>What is BCrypt?</b><br>
 * BCrypt is a strong, adaptive password hashing algorithm based on the Blowfish cipher.
 * It automatically incorporates a randomly generated "salt" into every hash to defend against
 * rainbow table attacks, and uses a configurable work factor (cost) to slow down brute-force attempts.
 *
 * <p>We register {@link PasswordEncoder} as a Spring Bean here so it can be injected
 * into {@link com.shopstack.service.AuthService} or any other component that needs to hash or verify passwords.
 */
@Configuration
public class PasswordConfig {

    /**
     * Creates a {@link BCryptPasswordEncoder} bean with default strength (log rounds = 10).
     *
     * @return a thread-safe {@link PasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
