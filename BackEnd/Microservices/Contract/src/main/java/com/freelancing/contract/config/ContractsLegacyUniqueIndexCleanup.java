package com.freelancing.contract.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Supprime l'ancien index UNIQUE sur {@code project_id} si présent (schéma JPA historique).
 * Sans cela, MySQL refuse un 2e contrat pour le même projet même après COMPLETED/CANCELLED → 400 sans corps côté Feign.
 */
@Component
@Order(Integer.MAX_VALUE)
public class ContractsLegacyUniqueIndexCleanup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContractsLegacyUniqueIndexCleanup.class);

    private final DataSource dataSource;

    public ContractsLegacyUniqueIndexCleanup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            String schema = conn.getCatalog();
            if (schema == null || schema.isBlank()) {
                return;
            }
            try (PreparedStatement ps =
                    conn.prepareStatement(
                            "SELECT INDEX_NAME FROM information_schema.STATISTICS "
                                    + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'contracts' "
                                    + "AND COLUMN_NAME = 'project_id' AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY'")) {
                ps.setString(1, schema);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String idx = rs.getString(1);
                        if (idx == null || idx.isBlank()) {
                            continue;
                        }
                        String safe = idx.replace("`", "").replace("\"", "");
                        try (Statement st = conn.createStatement()) {
                            st.execute("ALTER TABLE contracts DROP INDEX `" + safe + "`");
                            log.info("Removed legacy unique index on contracts.project_id: {}", safe);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Skipping contracts unique-index cleanup (non-MySQL or table missing): {}", e.getMessage());
        }
    }
}
