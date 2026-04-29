package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Aligne la table {@code users} sur l'entité JPA si elle provient d’un ancien schéma (ex. esprit)
 * ou si {@code baseline-on-migrate} a fait sauter {@code V1}.
 * Détection via {@code information_schema} (fiable sous MySQL).
 */
public class V3__EnsureBirthDateColumn extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "users")) {
            return;
        }
        try (Statement st = connection.createStatement()) {
            if (!columnExists(connection, "users", "birth_date")) {
                st.execute("ALTER TABLE users ADD COLUMN birth_date DATE NOT NULL DEFAULT '2000-01-01'");
            }
            if (!columnExists(connection, "users", "enabled")) {
                st.execute("ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1");
            }
            if (!columnExists(connection, "users", "face_descriptor")) {
                st.execute("ALTER TABLE users ADD COLUMN face_descriptor TEXT NULL");
            }
            if (!columnExists(connection, "users", "profile_picture_url")) {
                st.execute("ALTER TABLE users ADD COLUMN profile_picture_url VARCHAR(255) NULL");
            }
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
