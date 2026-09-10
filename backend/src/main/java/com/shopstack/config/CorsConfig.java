package com.shopstack.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CorsConfig — Cross-Origin Resource Sharing (CORS) Configuration.
 *
 * <p>In development the React dev server runs on {@code http://localhost:5173}.
 * In production set the {@code CORS_ALLOWED_ORIGINS} environment variable to your
 * deployed frontend URL (e.g. {@code https://shopstack.vercel.app}).
 *
 * <p>Multiple origins may be separated by commas:
 * {@code CORS_ALLOWED_ORIGINS=http://localhost:5173,https://your-frontend-domain.com}
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

    /**
     * Parses and cleans the comma-separated allowed origins.
     * Trims leading/trailing whitespace and removes trailing slashes.
     *
     * @return List of sanitized allowed origin strings/patterns.
     */
    public List<String> getAllowedOrigins() {
        if (allowedOriginsRaw == null || allowedOriginsRaw.trim().isEmpty()) {
            return List.of("http://localhost:5173");
        }
        return Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.replaceAll("/+$", ""))
                .collect(Collectors.toList());
    }

    /**
     * Spring Security CorsConfigurationSource bean.
     * Integrates with Spring Security's cors filter to handle preflight OPTIONS
     * and CORS headers before security authentication filters.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = getAllowedOrigins();
        log.info("[CorsConfig] Initializing Spring Security CORS with allowed origin patterns: {}", origins);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition", "Link", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = getAllowedOrigins();
        log.info("[CorsConfig] Initializing Spring MVC CORS with allowed origin patterns: {}", origins);

        registry.addMapping("/**")
                .allowedOriginPatterns(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition", "Link", "X-Total-Count")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

