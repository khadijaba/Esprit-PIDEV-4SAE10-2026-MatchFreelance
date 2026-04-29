package com.freelancing.interview.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Anciens schémas MySQL pouvaient avoir {@code status} en ENUM sans toutes les valeurs
 * ({@code SCHEDULED}, {@code COMPLETED}, …), ce qui provoque une erreur SQL à l’insertion.
 */
@Configuration
public class InterviewDatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(InterviewDatabaseConfiguration.class);

    @Bean
    @Order(0)
    @ConditionalOnProperty(name = "app.db.ensure-interview-status-varchar", havingValue = "true")
    ApplicationRunner ensureInterviewStatusColumnVarchar(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE `interviews` MODIFY COLUMN `status` VARCHAR(32) NOT NULL");
                log.info("Schéma interviews : colonne status alignée sur VARCHAR(32).");
            } catch (Exception e) {
                log.warn(
                        "Impossible d'aligner interviews.status en VARCHAR. Si la planification échoue (500), exécutez : "
                                + "ALTER TABLE `interviews` MODIFY COLUMN `status` VARCHAR(32) NOT NULL. Cause : {}",
                        e.getMessage());
            }
        };
    }
}
