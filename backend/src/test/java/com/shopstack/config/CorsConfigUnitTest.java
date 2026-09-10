package com.shopstack.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CorsConfigUnitTest {

    @Test
    @DisplayName("CORS Config — Parses comma-separated origins and trims whitespace and trailing slashes")
    void testOriginParsingWithCommaSeparatedValues() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOriginsRaw", "http://localhost:5173, https://shopstack.vercel.app/ , https://shopstack.com");

        List<String> origins = config.getAllowedOrigins();
        assertThat(origins).containsExactly(
                "http://localhost:5173",
                "https://shopstack.vercel.app",
                "https://shopstack.com"
        );
    }

    @Test
    @DisplayName("CORS Config — Defaults to localhost:5173 when property is null or empty")
    void testOriginParsingWithEmptyProperty() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOriginsRaw", "   ");

        List<String> origins = config.getAllowedOrigins();
        assertThat(origins).containsExactly("http://localhost:5173");
    }

    @Test
    @DisplayName("CORS Config — CorsConfigurationSource bean configures credentials and allowed methods")
    void testCorsConfigurationSourceBean() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOriginsRaw", "http://localhost:5173,https://shopstack.vercel.app");

        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);

        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowCredentials()).isTrue();
        assertThat(corsConfig.getAllowedOriginPatterns()).containsExactly("http://localhost:5173", "https://shopstack.vercel.app");
        assertThat(corsConfig.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
