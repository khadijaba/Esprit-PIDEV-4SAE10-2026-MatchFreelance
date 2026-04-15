package Config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SendGrid Configuration
 * 
 * Initializes the SendGrid client with API key from:
 * 1. Environment variable: SENDGRID_API_KEY
 * 2. Application property: sendgrid.api.key
 * 
 * Priority: Environment variable > Application property
 */
@Configuration
public class SendGridConfig {

    private static final Logger logger = LoggerFactory.getLogger(SendGridConfig.class);

    @Value("${sendgrid.api.key:${SENDGRID_API_KEY:}}")
    private String apiKey;

    @Bean
    public SendGrid sendGrid() {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("❌ SENDGRID API KEY NOT CONFIGURED");
            logger.error("Set one of these:");
            logger.error("  1. Environment variable: SENDGRID_API_KEY");
            logger.error("  2. Application property: sendgrid.api.key");
            throw new IllegalArgumentException("SendGrid API key is not configured");
        }

        logger.info("✓ SendGrid initialized with API key: {}...", apiKey.substring(0, Math.min(10, apiKey.length())));
        return new SendGrid(apiKey);
    }
}
