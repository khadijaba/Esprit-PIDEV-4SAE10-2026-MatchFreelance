package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Rend la colonne users.role compatible avec les rôles métier courants.
 * <p>
 * Certains schémas historiques ont un ENUM ne contenant pas PROJECT_OWNER,
 * ce qui provoque : "Data truncated for column 'role'" à l'inscription.
 * Cette migration force un type VARCHAR puis normalise CLIENT vers PROJECT_OWNER.
 */
public class V4__NormalizeRoleColumnType extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "users") || !columnExists(connection, "users", "role")) {
            return;
        }

        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(32) NOT NULL");
            st.executeUpdate("UPDATE users SET role = 'PROJECT_OWNER' WHERE role = 'CLIENT'");
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
