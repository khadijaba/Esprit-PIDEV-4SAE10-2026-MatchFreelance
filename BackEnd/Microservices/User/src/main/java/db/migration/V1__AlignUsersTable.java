package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ajoute les colonnes attendues par l'entité User (table {@code users}) si la table
 * existait déjà sans elles (erreur MySQL 1054 Unknown column 'birth_date', etc.).
 * Si la table n'existe pas encore, on ne fait rien : Hibernate ddl-auto=update la créera.
 */
public class V1__AlignUsersTable extends BaseJavaMigration {

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
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = connection.getCatalog();
        String[] types = { "TABLE" };
        if (hasRow(meta.getTables(catalog, null, table, types))) {
            return true;
        }
        return hasRow(meta.getTables(null, null, table, types));
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = connection.getCatalog();
        if (hasRow(meta.getColumns(catalog, null, table, column))) {
            return true;
        }
        return hasRow(meta.getColumns(null, null, table, column));
    }

    private static boolean hasRow(ResultSet rs) throws SQLException {
        try (ResultSet r = rs) {
            return r.next();
        }
    }
}
