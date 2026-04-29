package Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Corrige les dates invalides (ex. 0000-00-00) laissées par d'anciennes versions / imports MySQL.
 */
@Component
public class LegacyMysqlDateCleanup {

    private static final Logger log = LoggerFactory.getLogger(LegacyMysqlDateCleanup.class);

    private final JdbcTemplate jdbcTemplate;

    public LegacyMysqlDateCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    public void normalizeUserBirthDates() {
        try {
            int n = jdbcTemplate.update(
                    "UPDATE users SET birth_date = '1990-01-01' WHERE birth_date IS NULL OR YEAR(birth_date) = 0");
            if (n > 0) {
                log.warn("MySQL users.birth_date : {} ligne(s) corrigee(s) (date invalide / annee 0).", n);
            }
        } catch (Exception e) {
            log.debug("Nettoyage birth_date ignore (table absente ou deja conforme): {}", e.getMessage());
        }
    }
}
