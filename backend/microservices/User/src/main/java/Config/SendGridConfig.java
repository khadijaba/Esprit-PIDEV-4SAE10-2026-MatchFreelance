package Config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SendGrid — meme principe que PIDEV ; cle absente = client factice (dev validation).
 */
@Configuration
public class SendGridConfig {

    private static final Logger logger = LoggerFactory.getLogger(SendGridConfig.class);

    @Value("${sendgrid.api.key:${SENDGRID_API_KEY:}}")
    private String apiKey;

    @Bean
    public SendGrid sendGrid() {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("SendGrid API key absente — client factice (echecs d'envoi attendus en local)");
            return new SendGrid("SG.unconfigured-local");
        }

        logger.info("SendGrid initialise (prefixe cle): {}...", apiKey.substring(0, Math.min(10, apiKey.length())));
        return new SendGrid(apiKey);
    }
}
