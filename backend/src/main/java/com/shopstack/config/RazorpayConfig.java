package com.shopstack.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * RazorpayConfig — Spring Configuration providing a {@link RazorpayClient} bean.
 *
 * <p>Reads API credentials from application.properties via environment variables.
 * Keys must be set as environment variables:
 * <pre>
 *   RAZORPAY_KEY_ID     = your Razorpay key ID
 *   RAZORPAY_KEY_SECRET = your Razorpay key secret
 * </pre>
 *
 * <p><b>Startup resilience:</b> If credentials are missing or are the known local
 * placeholder values, the bean initialisation logs a warning instead of crashing the
 * application context. Payment API calls will fail gracefully with a clear error
 * message rather than preventing the rest of the application from starting.
 */
@Configuration
public class RazorpayConfig {

    private static final Logger log = LoggerFactory.getLogger(RazorpayConfig.class);

    /** Known local/test placeholder key ID — never make real calls with this value. */
    private static final String PLACEHOLDER_KEY_ID = "rzp_test_mockkeyid";

    @Value("${razorpay.key.id:}")
    private String keyId;

    @Value("${razorpay.key.secret:}")
    private String keySecret;

    @Autowired(required = false)
    private Environment environment;

    /**
     * Exposes RazorpayClient as a Spring-managed Bean.
     *
     * <p>If credentials are not configured, returns a stand-in client that
     * allows the application context to start cleanly. Payment creation requests
     * validate credentials explicitly before attempting gateway transactions.
     *
     * @return configured {@link RazorpayClient}
     */
    @Bean
    public RazorpayClient razorpayClient() {
        String cleanKeyId = keyId != null ? keyId.trim().replaceAll("[\"'\r\n]", "") : "";
        String cleanKeySecret = keySecret != null ? keySecret.trim().replaceAll("[\"'\r\n]", "") : "";

        boolean isKeyConfigured = !cleanKeyId.isEmpty()
                && !PLACEHOLDER_KEY_ID.equalsIgnoreCase(cleanKeyId)
                && !"rzp_test_placeholder".equalsIgnoreCase(cleanKeyId);

        boolean isSecretConfigured = !cleanKeySecret.isEmpty()
                && !"placeholder_secret".equalsIgnoreCase(cleanKeySecret)
                && !"mocksecret123456789".equalsIgnoreCase(cleanKeySecret)
                && !"your_razorpay_key_secret_here".equalsIgnoreCase(cleanKeySecret);

        String keyType = cleanKeyId.startsWith("rzp_test_") ? "TEST"
                : (cleanKeyId.startsWith("rzp_live_") ? "LIVE" : (isKeyConfigured ? "CUSTOM" : "NONE"));

        String activeProfile = "default";
        if (environment != null && environment.getActiveProfiles() != null && environment.getActiveProfiles().length > 0) {
            activeProfile = String.join(", ", environment.getActiveProfiles());
        }

        log.info("[RazorpayConfig]\nRazorpay configuration:\nProfile: {}\nKey configured: {}\nKey type: {}\nSecret configured: {}",
                activeProfile, isKeyConfigured, keyType, isSecretConfigured);

        if (!isKeyConfigured || !isSecretConfigured) {
            log.warn(
                "[RazorpayConfig] Razorpay credentials are not configured or are placeholder values. "
                + "Razorpay Key ID configured: {}, Razorpay Key Secret configured: {}. "
                + "Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables to enable payments.",
                isKeyConfigured,
                isSecretConfigured
            );
            try {
                return new RazorpayClient("rzp_test_placeholder", "placeholder_secret");
            } catch (RazorpayException e) {
                log.error("[RazorpayConfig] Could not initialise RazorpayClient stand-in: {}", e.getMessage());
                throw new IllegalStateException("Razorpay client could not be initialised.", e);
            }
        }

        try {
            RazorpayClient client = new RazorpayClient(cleanKeyId, cleanKeySecret);
            log.info("[RazorpayConfig] RazorpayClient initialised successfully (Type: {}, Secret configured: true)", keyType);
            return client;
        } catch (RazorpayException e) {
            log.error("[RazorpayConfig] Failed to initialise RazorpayClient: {}", e.getMessage());
            throw new IllegalStateException("Razorpay client initialisation failed: " + e.getMessage(), e);
        }
    }
}

