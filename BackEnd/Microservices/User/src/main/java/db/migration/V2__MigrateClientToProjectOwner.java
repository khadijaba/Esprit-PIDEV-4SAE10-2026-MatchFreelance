package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Le rôle métier unique pour le porteur de projet est {@code PROJECT_OWNER}.
 * Les comptes historiques en {@code CLIENT} sont alignés avant la suppression de l'enum {@code CLIENT}.
 */
public class V2__MigrateClientToProjectOwner extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "users")) {
            return;
        }
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("UPDATE users SET user_role = 'PROJECT_OWNER' WHERE user_role = 'CLIENT'");
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

    private static boolean hasRow(ResultSet rs) throws SQLException {
        try (ResultSet r = rs) {
            return r.next();
        }
    }
}
