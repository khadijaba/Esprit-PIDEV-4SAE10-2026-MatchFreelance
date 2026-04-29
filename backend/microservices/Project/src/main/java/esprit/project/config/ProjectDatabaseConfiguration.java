package esprit.project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Anciens schémas MySQL pouvaient avoir {@code status} en ENUM sans valeur {@code DRAFT},
 * ce qui provoquait une erreur SQL (souvent 500) à la création d'un brouillon.
 * <p>
 * Activé via {@code app.db.ensure-status-varchar=true} (déjà défini dans Config Server PROJECT.properties).
 */
@Configuration
public class ProjectDatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ProjectDatabaseConfiguration.class);

    @Bean
    @Order(0)
    @ConditionalOnProperty(name = "app.db.ensure-status-varchar", havingValue = "true")
    ApplicationRunner ensureProjectStatusColumnVarchar(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE `project` MODIFY COLUMN `status` VARCHAR(32) NOT NULL");
                log.info("Schéma projet : colonne status alignée sur VARCHAR(32) (valeurs DRAFT, OPEN, …).");
            } catch (Exception e) {
                log.warn(
                        "Impossible d'aligner project.status en VARCHAR. Si l'enregistrement DRAFT échoue (500), "
                                + "exécutez en MySQL : ALTER TABLE `project` MODIFY COLUMN `status` VARCHAR(32) NOT NULL. "
                                + "Cause : {}",
                        e.getMessage());
            }
        };
    }
}
