package com.shopstack.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CorsConfig — Cross-Origin Resource Sharing (CORS) Configuration.
 *
 * <p>In development the React dev server runs on {@code http://localhost:5173}.
 * In production set the {@code CORS_ALLOWED_ORIGINS} environment variable to your
 * deployed frontend URL (e.g. {@code https://shopstack.example.com}).
 *
 * <p>Multiple origins may be separated by commas:
 * {@code CORS_ALLOWED_ORIGINS=https://shopstack.example.com,https://www.shopstack.example.com}
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    /**
     * Comma-separated list of allowed origins.
     * Defaults to the React dev server when the environment variable is not set.
     */
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsRaw;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOriginsRaw.split(",");
        // Trim whitespace from each origin
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }
        log.info("[CorsConfig] Allowed origins: {}", java.util.Arrays.toString(origins));

        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
