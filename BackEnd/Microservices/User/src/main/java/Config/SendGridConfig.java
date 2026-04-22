package Config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * SendGrid : la clé API se configure dans l'application (ce n'est pas suffisant de la créer sur sendgrid.com).
 * Ordre utilisé : propriété {@code sendgrid.api.key} si non vide, sinon variable d'environnement {@code SENDGRID_API_KEY}.
 */
@Configuration
public class SendGridConfig {

    private static final Logger logger = LoggerFactory.getLogger(SendGridConfig.class);

    @Value("${sendgrid.api.key:}")
    private String apiKeyFromProperties;

    @Value("${SENDGRID_API_KEY:}")
    private String apiKeyFromEnv;

    @Bean
    public SendGrid sendGrid() {
        String apiKey = StringUtils.hasText(apiKeyFromProperties)
                ? apiKeyFromProperties.trim()
                : (apiKeyFromEnv != null ? apiKeyFromEnv.trim() : "");

        if (!StringUtils.hasText(apiKey)) {
            logger.warn("[SendGrid] API key not configured");
            logger.warn("Copiez la clé depuis SendGrid (API Keys) puis :");
            logger.warn("  • collez-la dans sendgrid.api.key (application.properties / Config Server), ou");
            logger.warn("  • définissez la variable d'environnement SENDGRID_API_KEY");
            logger.warn("Le microservice USER démarre sans envoi email tant qu'aucune clé n'est fournie.");
            return new SendGrid("SENDGRID_DISABLED_NO_API_KEY");
        }

        logger.info("[SendGrid] Initialized with API key prefix: {}...", apiKey.substring(0, Math.min(10, apiKey.length())));
        return new SendGrid(apiKey);
    }
}
