package esprit.project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Les anciennes bases MySQL peuvent avoir {@code status} en ENUM sans valeur DRAFT,
 * ce qui provoque une erreur SQL à l'INSERT. On force VARCHAR pour accepter tous les {@link esprit.project.entities.ProjectStatus}.
 */
@Configuration
public class ProjectStatusColumnMigration {

    private static final Logger log = LoggerFactory.getLogger(ProjectStatusColumnMigration.class);

    @Bean
    @Order(Integer.MAX_VALUE)
    ApplicationRunner ensureProjectStatusAsVarchar(JdbcTemplate jdbcTemplate, Environment env) {
        return args -> {
            String url = env.getProperty("spring.datasource.url", "");
            if (url == null || !url.toLowerCase().contains("mysql")) {
                return;
            }
            if (!env.getProperty("app.db.ensure-status-varchar", Boolean.class, Boolean.TRUE)) {
                return;
            }
            try {
                jdbcTemplate.execute("ALTER TABLE project MODIFY COLUMN status VARCHAR(32) NOT NULL");
                log.info("Colonne project.status vérifiée / convertie en VARCHAR(32).");
            } catch (Exception ex) {
                log.warn(
                        "Migration automatique project.status ignorée (table absente, nom différent, droits, ou déjà OK): {}",
                        ex.getMessage());
            }
        };
    }
}
